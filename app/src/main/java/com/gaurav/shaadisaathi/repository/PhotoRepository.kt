package com.gaurav.shaadisaathi.repository

import com.gaurav.shaadisaathi.models.Photo
import com.gaurav.shaadisaathi.models.PhotoAlbum
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PhotoRepository {

    private val database = FirebaseDatabase.getInstance()
    private val albumsRef = database.getReference("photoAlbums")
    private val photosRef = database.getReference("photos")

    suspend fun getAlbumById(albumId: String): Result<PhotoAlbum?> {
        return suspendCancellableCoroutine { continuation ->
            albumsRef.child(albumId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val album = snapshot.getValue(PhotoAlbum::class.java)
                        continuation.resume(Result.success(album))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun getAllAlbums(): Result<List<PhotoAlbum>> {
        return suspendCancellableCoroutine { continuation ->
            albumsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val albums = mutableListOf<PhotoAlbum>()
                        for (childSnapshot in snapshot.children) {
                            val album = childSnapshot.getValue(PhotoAlbum::class.java)
                            album?.let { albums.add(it) }
                        }
                        continuation.resume(Result.success(albums))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun getPhotosByAlbumId(albumId: String): Result<List<Photo>> {
        return suspendCancellableCoroutine { continuation ->
            photosRef.orderByChild("albumId").equalTo(albumId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val photos = mutableListOf<Photo>()
                            for (childSnapshot in snapshot.children) {
                                val photo = childSnapshot.getValue(Photo::class.java)
                                photo?.let { photos.add(it) }
                            }
                            continuation.resume(Result.success(photos))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                })
        }
    }

    suspend fun getRecentPhotos(limit: Int = 20): Result<List<Photo>> {
        return suspendCancellableCoroutine { continuation ->
            photosRef.orderByChild("uploadedAt").limitToLast(limit)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val photos = mutableListOf<Photo>()
                            for (childSnapshot in snapshot.children) {
                                val photo = childSnapshot.getValue(Photo::class.java)
                                photo?.let { photos.add(it) }
                            }
                            // Reverse to get most recent first
                            photos.reverse()
                            continuation.resume(Result.success(photos))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                })
        }
    }

    suspend fun addPhoto(photo: Photo): Result<String> {
        return try {
            photosRef.child(photo.id).setValue(photo)
            Result.success(photo.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhoto(photo: Photo): Result<Boolean> {
        return try {
            photosRef.child(photo.id).setValue(photo)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhoto(photoId: String): Result<Boolean> {
        return try {
            photosRef.child(photoId).removeValue()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAlbum(album: PhotoAlbum): Result<String> {
        return try {
            albumsRef.child(album.id).setValue(album)
            Result.success(album.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAlbum(album: PhotoAlbum): Result<Boolean> {
        return try {
            albumsRef.child(album.id).setValue(album)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAlbum(albumId: String): Result<Boolean> {
        return try {
            // First delete all photos in the album
            val photosResult = getPhotosByAlbumId(albumId)
            if (photosResult.isSuccess) {
                val photos = photosResult.getOrNull() ?: emptyList()
                photos.forEach { photo ->
                    photosRef.child(photo.id).removeValue()
                }
            }

            // Then delete the album
            albumsRef.child(albumId).removeValue()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhotoById(photoId: String): Result<Photo?> {
        return suspendCancellableCoroutine { continuation ->
            photosRef.child(photoId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val photo = snapshot.getValue(Photo::class.java)
                        continuation.resume(Result.success(photo))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun searchPhotos(query: String): Result<List<Photo>> {
        return suspendCancellableCoroutine { continuation ->
            photosRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val photos = mutableListOf<Photo>()
                                                    for (childSnapshot in snapshot.children) {
                                val photo = childSnapshot.getValue(Photo::class.java)
                                photo?.let {
                                    if (it.caption.contains(query, ignoreCase = true) ||
                                        it.uploaderName.contains(query, ignoreCase = true)) {
                                        photos.add(it)
                                    }
                                }
                            }
                        continuation.resume(Result.success(photos))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    suspend fun getPhotosByUploader(uploaderId: String): Result<List<Photo>> {
        return suspendCancellableCoroutine { continuation ->
            photosRef.orderByChild("uploaderId").equalTo(uploaderId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val photos = mutableListOf<Photo>()
                            for (childSnapshot in snapshot.children) {
                                val photo = childSnapshot.getValue(Photo::class.java)
                                photo?.let { photos.add(it) }
                            }
                            continuation.resume(Result.success(photos))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                })
        }
    }

    suspend fun updateAlbumPhotoCount(albumId: String): Result<Boolean> {
        return try {
            val photosResult = getPhotosByAlbumId(albumId)
            if (photosResult.isSuccess) {
                val photoCount = photosResult.getOrNull()?.size ?: 0
                val albumResult = getAlbumById(albumId)
                if (albumResult.isSuccess) {
                    val album = albumResult.getOrNull()
                    album?.let {
                        val updatedAlbum = it.copy(
                            photoCount = photoCount
                        )
                        albumsRef.child(albumId).setValue(updatedAlbum)
                    }
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbumsByCreator(creatorId: String): Result<List<PhotoAlbum>> {
        return suspendCancellableCoroutine { continuation ->
            albumsRef.orderByChild("hostId").equalTo(creatorId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            val albums = mutableListOf<PhotoAlbum>()
                            for (childSnapshot in snapshot.children) {
                                val album = childSnapshot.getValue(PhotoAlbum::class.java)
                                album?.let { albums.add(it) }
                            }
                            continuation.resume(Result.success(albums))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                })
        }
    }
}
