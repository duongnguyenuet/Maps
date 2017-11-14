package binary.maps.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import binary.maps.models.Place;

/**
 * Created by duong on 11/7/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "Locations";
    public static final String KEY_ID = "id";
    public static final String LATITUDE = "latitude";
    public static final String LONGTITUDE = "longtitude";

    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }



    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_LOCATION_DATABASE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + LATITUDE + " TEXT,"
                + LONGTITUDE + " TEXT" + ")";
        sqLiteDatabase.execSQL(CREATE_LOCATION_DATABASE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int newVersion, int oldVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public void addLocation(String latitude, String longitude){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LATITUDE, latitude);
        values.put(LONGTITUDE, longitude);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public List<Place> getLocation(){
        List<Place> locationList = new ArrayList<Place>();

        String selectQuery = "SELECT  * FROM " + TABLE_NAME;

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do {
                Place location = new Place();
                location.setId(Integer.parseInt(cursor.getString(0)));
                location.setLat(Double.parseDouble(cursor.getString(1)));
                location.setLng(Double.parseDouble(cursor.getString(2)));

                locationList.add(location);
            }while (cursor.moveToNext());
        }
        cursor.close();
        return locationList;
    }
}
