package com.example.assetsbasecontentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/////////////

// Пример реализации ContentProvider для базы данных, созданной вне приложения.

////////////


public class AssetsBaseContentProvider extends ContentProvider {
    SQLiteDatabase db;
    Helper helper;
    Handler handler;
    Cursor cursor;

    static final String AUTHORITY = "AssetsBaseContentProvider";
    static final String MUSHROOMS_PATH = "myMushrooms";
    static final String MUSHROOMS_TABLE = "mushrooms"; // имя таблицы базы. Их может быть больше.
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MUSHROOMS_PATH); // URI по умолчанию. По этому URI можно будет получить все записи таблицы mushrooms
    static final int URI_MUSHROOMS = 1;                                                               // Типы данных, которые мы хотим получить. В случае с базой, в которой 1 таблица,
    static final int URI_MUSHROOMS_ID = 2;                                                            // это либо несколько значений (может быть уточнено в selection), либо 1 значение

    static final String ASSETS_DB_NAME = "TestDB";                                                    // название базы из папки assets

    private static final UriMatcher uriMatcher;                                                       // Определяет типы данных, которые мы хотим получить, по входящим uri
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, MUSHROOMS_PATH, URI_MUSHROOMS);
        uriMatcher.addURI(AUTHORITY, MUSHROOMS_PATH + "/#", URI_MUSHROOMS_ID);
    }

    static final String MUSHROOMS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + MUSHROOMS_PATH;         // для метода getType
    static final String MUSHROOMS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd." + AUTHORITY + "." + MUSHROOMS_PATH;

    public AssetsBaseContentProvider() {
    }

    @Override
    public String getType(Uri uri) {
        String type = null;
        switch (uriMatcher.match(uri)) {
            case URI_MUSHROOMS:
                type = MUSHROOMS_CONTENT_TYPE;
                break;
            case URI_MUSHROOMS_ID:
                type = MUSHROOMS_CONTENT_ITEM_TYPE;
                break;
        }
        return type;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        helper = new Helper(getContext(), "DB", null, 1);
        db = helper.getMyDataBase(ASSETS_DB_NAME);                                  // в каждом методе получаем базу заново

        switch (uriMatcher.match(uri)) {
            case URI_MUSHROOMS:
                if (TextUtils.isEmpty(sortOrder)) sortOrder = "_id";                // если это несколько строк, и если сортировка не указана, можем указать дефолтную
                break;
            case URI_MUSHROOMS_ID:                                                  // если это одна строка, включаем _id строки в условие
               if (selection == null)
                    selection = " _id = " + uri.getLastPathSegment();
                else
                    selection = selection + " AND _id = " + uri.getLastPathSegment();
                break;
        }
        cursor = db.query(MUSHROOMS_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), CONTENT_URI);                  // подписывем курсор на изменения по нашему URI
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        helper = new Helper(getContext(), "DB", null, 1);
        db = helper.getMyDataBase(ASSETS_DB_NAME);

        switch (uriMatcher.match(uri)) {
            case URI_MUSHROOMS:

                break;
            case URI_MUSHROOMS_ID:
                if (selection == null)
                    selection = " _id = " + uri.getLastPathSegment();
                else
                    selection = selection + " AND _id = " + uri.getLastPathSegment();
                break;
        }
        int number = db.update(MUSHROOMS_TABLE,values,selection,selectionArgs);                                            // возвращаем количество затронутых рядов
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);                                        // сообщаем курсору, что по нашему uri произошли изменения
        return number;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        helper = new Helper(getContext(), "DB", null, 1);
        db = helper.getMyDataBase(ASSETS_DB_NAME);

        if (uri.equals(CONTENT_URI))
        {
           long _id = db.insert(MUSHROOMS_TABLE,null,values);
           getContext().getContentResolver().notifyChange(CONTENT_URI, null);                                  // сообщаем курсору, что по нашему uri произошли изменения
           return Uri.parse(uri.toString() + "/" +  _id);                                                               // возвращаем uri для получения добавляемого элемента
        }
        else
            return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        helper = new Helper(getContext(), "DB", null, 1);
        db = helper.getMyDataBase(ASSETS_DB_NAME);

        switch (uriMatcher.match(uri)) {
            case URI_MUSHROOMS:

                break;
            case URI_MUSHROOMS_ID:
                if (selection == null)
                    selection = " _id = " + uri.getLastPathSegment();
                else
                    selection = selection + " AND _id = " + uri.getLastPathSegment();
                break;
        }
        int number = db.delete(MUSHROOMS_TABLE, selection, selectionArgs);                                              // возвращаем количество затронутых рядов
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);                                     // сообщаем курсору, что по нашему uri произошли изменения
        return number;
    }

    static class Helper extends SQLiteOpenHelper
    {
        Context contextCP;
        Helper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version)  {
            super(context, name, factory, version);
            contextCP = context;
        }

       synchronized SQLiteDatabase getMyDataBase(String assetsDBName)
        {
            SQLiteDatabase myDataBase;
            boolean isBaseCreated;                          // будем создавать базу, если она еще не создана, и копировать туда наш файл из accets ( OutputStream  ---->  InputStream )
            try
            {
                SQLiteDatabase.openDatabase("/data/user/0/com.example.assetsbasecontentprovider/databases/DB", null, SQLiteDatabase.OPEN_READONLY); //  на старых версиях андроида может быть другой путь !!!
                isBaseCreated = true;
            }
            catch (SQLiteCantOpenDatabaseException e) {isBaseCreated = false;}

            if (isBaseCreated)
            {
                return this.getReadableDatabase();                          // если создана - возвращаем ее
            }
            else {
                try {
                    myDataBase = this.getReadableDatabase();
                    myDataBase.close();                                     // закрываем базу, в которую будем копировать перед тем, как начнем. иначе на 9 версии андроида всегда ошибки.
                    String myDataBasePath = myDataBase.getPath();

                    InputStream inputStream;
                    OutputStream outputStream;

                    assert contextCP != null;
                    inputStream = contextCP.getAssets().open(assetsDBName);
                    outputStream = new FileOutputStream(myDataBasePath);

                    int length;
                    byte[] buffer = new byte[1024];

                    while ((length = inputStream.read(buffer)) > 0)
                    {
                        outputStream.write(buffer, 0, length);
                    }
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                } catch (IOException e) {
                    Log.d("log", Objects.requireNonNull(e.getLocalizedMessage()));
                }
            }

            return this.getReadableDatabase();      // вернуть myDataBase напрямую не можем, потомучто она закрыта, а getReadableDatabase() возвращает готовую у работе, но это она и есть
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d("LOG", "onCreate");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d("LOG", "onUpgrade");
        }
    }
}
