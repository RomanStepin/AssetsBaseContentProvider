package com.example.assetsbasecontentprovider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    Button buttonInsert;
    Button buttonUpdate;
    Button buttonDelete;

    TextView textViewInsertValue;
    TextView textViewUpdateValue;
    TextView textViewUpdateNumber;
    TextView textViewDeleteNumber;

    ListView listView;
    SimpleCursorAdapter simpleCursorAdapter;

    ContentResolver contentResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonDelete = findViewById(R.id.button_delete);
        buttonInsert = findViewById(R.id.button_insert);
        buttonUpdate = findViewById(R.id.button_update);

        textViewDeleteNumber = findViewById(R.id.editText_delete_number);
        textViewInsertValue = findViewById(R.id.editText_insert_value);
        textViewUpdateNumber = findViewById(R.id.editText_update_number);
        textViewUpdateValue = findViewById(R.id.editText_update_value);

        listView = findViewById(R.id.listView);
        simpleCursorAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, null, new String[]{"name", "_id"}, new int[]{android.R.id.text1, android.R.id.text2},0);
        listView.setAdapter(simpleCursorAdapter);

        buttonInsert.setOnClickListener(this);
        buttonUpdate.setOnClickListener(this);
        buttonDelete.setOnClickListener(this);

        Bundle bundle = new Bundle();
        bundle.putString("uri", "content://AssetsBaseContentProvider/myMushrooms");
        getSupportLoaderManager().initLoader(0, bundle, this);

        contentResolver = getContentResolver();

    }

    @Override
    public void onClick(View v) {
        String id;
        ContentValues contentValues;
        switch (v.getId())
        {
            case R.id.button_delete:
                id = textViewDeleteNumber.getText().toString();
                contentResolver.delete(Uri.parse("content://AssetsBaseContentProvider/myMushrooms/"+id),null,null);
                Objects.requireNonNull(getSupportLoaderManager().getLoader(0)).forceLoad();
                break;
            case R.id.button_update:
                contentValues = new ContentValues();
                id = textViewUpdateNumber.getText().toString();
                contentValues.put("name", textViewUpdateValue.getText().toString());
                contentValues.put("edible", 1);
                contentResolver.update(Uri.parse("content://AssetsBaseContentProvider/myMushrooms/" + id), contentValues, null, null);
                Objects.requireNonNull(getSupportLoaderManager().getLoader(0)).forceLoad();
                break;
            case R.id.button_insert:
                contentValues = new ContentValues();
                contentValues.put("name", textViewInsertValue.getText().toString());
                contentValues.put("edible", 1);
                contentResolver.insert(Uri.parse("content://AssetsBaseContentProvider/myMushrooms"), contentValues);
                Objects.requireNonNull(getSupportLoaderManager().getLoader(0)).forceLoad();
                break;
        }
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new MyCursorLoader(this, Uri.parse(args.getString("uri")), args.getStringArray("projection"),
                args.getString("selection"), args.getStringArray("selectionArgs"),args.getString("sortOrder"));
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        simpleCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

   static class MyCursorLoader extends CursorLoader
    {
        Context context;
        Uri uri;
        String[] projection;
        String selection;
        String[] selectionArgs;
        String sortOrder;

        MyCursorLoader(@NonNull Context context, @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
            super(context, uri, projection, selection, selectionArgs, sortOrder);
            this.context = context;
            this.uri = uri;
            this.projection = projection;
            this.selection = selection;
            this.selectionArgs = selectionArgs;
            this.sortOrder = sortOrder;
        }

        @Override
        public Cursor loadInBackground() {
            return context.getContentResolver().query(uri,projection,selection,selectionArgs,sortOrder);
        }
    }
}
