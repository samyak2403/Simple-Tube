package com.samyak.simpletube.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.samyak.simpletube.constants.AlbumFilter
import com.samyak.simpletube.constants.AlbumSortType
import com.samyak.simpletube.db.entities.Album
import com.samyak.simpletube.db.entities.AlbumArtistMap
import com.samyak.simpletube.db.entities.AlbumEntity
import com.samyak.simpletube.db.entities.AlbumWithSongs
import com.samyak.simpletube.db.entities.ArtistEntity
import com.samyak.simpletube.db.entities.Song
import com.samyak.simpletube.db.entities.SongAlbumMap
import com.samyak.simpletube.extensions.reversed
import com.zionhuang.innertube.models.AlbumItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/*
 * Logic related to albums entities and their mapping
 */

@Dao
interface AlbumsDao : ArtistsDao {

    // region Gets
    @Transaction
    @Query("""
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album
            LEFT JOIN song ON song.albumId = album.id
        WHERE album.id = :id
        GROUP BY album.id
    """)
    fun album(id: String): Flow<Album?>

    @Transaction
    @Query("""
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album
            LEFT JOIN song ON song.albumId = album.id
        WHERE album.title LIKE '%' || :query || '%' AND song.inLibrary IS NOT NULL
        GROUP BY album.id
        LIMIT :previewSize
    """)
    fun searchAlbums(query: String, previewSize: Int = Int.MAX_VALUE): Flow<List<Album>>

    @Transaction
    @Query("""
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album
            LEFT JOIN song ON song.albumId = album.id
        WHERE album.id = :albumId
        GROUP BY album.id
    """)
    fun albumWithSongs(albumId: String): Flow<AlbumWithSongs?>

    @Transaction
    @Query("SELECT song.* FROM song JOIN song_album_map ON song.id = song_album_map.songId WHERE song_album_map.albumId = :albumId")
    fun albumSongs(albumId: String): Flow<List<Song>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT *, count(song.dateDownload) downloadCount
        FROM album
            JOIN song ON album.id = song.albumId
            JOIN event ON song.id = event.songId
        WHERE event.timestamp > :fromTimeStamp
        GROUP BY album.id
        ORDER BY SUM(event.playTime) DESC
        LIMIT :limit OFFSET :offset;
    """)
    fun mostPlayedAlbums(fromTimeStamp: Long, limit: Int = 6, offset: Int = 0): Flow<List<Album>>

    @Transaction
    @Query("""
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album_artist_map 
            JOIN album ON album_artist_map.albumId = album.id
            JOIN song ON album_artist_map.albumId = song.albumId
        WHERE artistId = :artistId
        GROUP BY album.id
        LIMIT :previewSize
    """)
    fun artistAlbumsPreview(artistId: String, previewSize: Int = 6): Flow<List<Album>>

    @RawQuery(observedEntities = [AlbumEntity::class])
    fun _getAlbum(query: SupportSQLiteQuery): Flow<List<Album>>

    fun albums(filter: AlbumFilter, sortType: AlbumSortType, descending: Boolean): Flow<List<Album>> {
        val orderBy = when (sortType) {
            AlbumSortType.CREATE_DATE -> "album.rowId ASC"
            AlbumSortType.NAME -> "album.title COLLATE NOCASE ASC"
            AlbumSortType.ARTIST -> """(
                                        SELECT LOWER(GROUP_CONCAT(name, ''))
                                        FROM artist
                                        WHERE id IN (SELECT artistId FROM album_artist_map WHERE albumId = album.id)
                                        ORDER BY name
                                    ) COLLATE NOCASE ASC"""
            AlbumSortType.YEAR -> "album.year ASC"
            AlbumSortType.SONG_COUNT -> "album.songCount ASC"
            AlbumSortType.LENGTH -> "album.duration ASC"
            AlbumSortType.PLAY_TIME -> "SUM(song.totalPlayTime) ASC"
        }

        val where = when (filter) {
            AlbumFilter.DOWNLOADED -> "song.dateDownload IS NOT NULL"
            AlbumFilter.LIBRARY -> "song.inLibrary IS NOT NULL"
            AlbumFilter.LIKED -> "album.bookmarkedAt IS NOT NULL"
        }

        val query = SimpleSQLiteQuery("""
            SELECT album.*, count(song.dateDownload) downloadCount
            FROM album
                LEFT JOIN song ON song.albumId = album.id
            WHERE $where
            GROUP BY album.id
            ORDER BY $orderBy
        """)

        return _getAlbum(query).map { it.reversed(descending) }
    }

    fun albumsInLibraryAsc() = albums(AlbumFilter.LIBRARY, AlbumSortType.CREATE_DATE, false)
    fun albumsLikedAsc() = albums(AlbumFilter.LIKED, AlbumSortType.CREATE_DATE, false)

    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT n.songId      AS eid,
                     SUM(playTime) AS oldPlayTime,
                     newPlayTime
              FROM event
                       JOIN
                   (SELECT songId, SUM(playTime) AS newPlayTime
                    FROM event
                    WHERE timestamp > (:now - 86400000 * 30 * 1)
                    GROUP BY songId
                    ORDER BY newPlayTime) as n
                   ON event.songId = n.songId
              WHERE timestamp < (:now - 86400000 * 30 * 1)
              GROUP BY n.songId
              ORDER BY oldPlayTime) AS t
                 JOIN song on song.id = t.eid
        WHERE 0.2 * t.oldPlayTime > t.newPlayTime
        LIMIT 100
    """
    )
    fun forgottenFavorites(now: Long = System.currentTimeMillis()): Flow<List<Song>>
    @Transaction
    @Query(
        """
        SELECT song.*
        FROM event
                 JOIN
             song ON event.songId = song.id
        WHERE event.timestamp > (:now - 86400000 * 7 * 2)
        GROUP BY song.albumId
        HAVING song.albumId IS NOT NULL
        ORDER BY sum(event.playTime) DESC
        LIMIT :limit
        OFFSET :offset
        """,
    )
    fun recommendedAlbum(
        now: Long = System.currentTimeMillis(),
        limit: Int = 5,
        offset: Int = 0,
    ): Flow<List<Song>>
    // endregion

    // region Inserts
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: AlbumArtistMap)

    @Transaction
    fun insert(albumItem: AlbumItem) {
        if (insert(AlbumEntity(
                id = albumItem.browseId,
                playlistId = albumItem.playlistId,
                title = albumItem.title,
                year = albumItem.year,
                thumbnailUrl = albumItem.thumbnail,
                songCount = 0,
                duration = 0
            )) == -1L
        ) return
        albumItem.artists
            ?.map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId(),
                    name = artist.name
                )
            }
            ?.onEach(::insert)
            ?.mapIndexed { index, artist ->
                AlbumArtistMap(
                    albumId = albumItem.browseId,
                    artistId = artist.id,
                    order = index
                )
            }
            ?.forEach(::insert)
    }
    // endregion

    // region Updates
    @Update
    fun update(album: AlbumEntity)

    @Upsert
    fun upsert(map: SongAlbumMap)

    @Transaction
    @Query("UPDATE album_artist_map SET artistId = :newId WHERE artistId = :oldId")
    fun updateAlbumArtistMap(oldId: String, newId: String)

    @Transaction
    @Query("DELETE FROM song_artist_map WHERE songId = :songID")
    fun unlinkSongArtists(songID: String)
    // endregion

    // region Deletes
    @Delete
    fun delete(album: AlbumEntity)

    @Transaction
    @Query("DELETE FROM album WHERE isLocal = 1")
    fun nukeLocalAlbums()
    // endregion
}