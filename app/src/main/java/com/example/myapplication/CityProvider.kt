package com.example.myapplication

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.content.UriMatcher

class CityProvider : ContentProvider() {

    private lateinit var dbHelper: DatabaseHelper

    companion object {
        private const val AUTHORITY = "com.example.myapplication.provider"
        private const val TABLE_NAME = "cities"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_NAME")
        private const val CITIES = 1
        private const val CITY_ID = 2
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, TABLE_NAME, CITIES)
            addURI(AUTHORITY, "$TABLE_NAME/#", CITY_ID)
        }
    }

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = when (uriMatcher.match(uri)) {
            CITIES -> db.query(DatabaseHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            CITIES -> {
                val id = db.insert(DatabaseHelper.TABLE_NAME, null, values)
                if (id > -1) {
                    val newUri = ContentUris.withAppendedId(CONTENT_URI, id)
                    context!!.contentResolver.notifyChange(newUri, null)
                    newUri
                } else {
                    null
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        val rowsDeleted = when (uriMatcher.match(uri)) {
            CITIES -> db.delete(DatabaseHelper.TABLE_NAME, selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        if (rowsDeleted > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        return rowsDeleted
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val db = dbHelper.writableDatabase
        val rowsUpdated = when (uriMatcher.match(uri)) {
            CITIES -> db.update(DatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        if (rowsUpdated > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        return rowsUpdated
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CITIES -> "vnd.android.cursor.dir/$AUTHORITY.$TABLE_NAME"
            CITY_ID -> "vnd.android.cursor.item/$AUTHORITY.$TABLE_NAME"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}