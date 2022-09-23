package com.reactnativecloudinarysdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.core.util.Pair;

import com.cloudinary.utils.StringUtils;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class Utils {
  static public WritableArray listToWritableArray(List list) {
    WritableArray writableArray = new WritableNativeArray();

    for (Object value : list) {
      if (value instanceof String) {
        writableArray.pushString((String) value);
      } else if (value instanceof Integer) {
        writableArray.pushInt((Integer) value);
      } else if (value instanceof Float || value instanceof Double) {
        writableArray.pushDouble((Double) value);
      } else if (value instanceof Boolean) {
        writableArray.pushBoolean((Boolean) value);
      } else if (value instanceof Map) {
        writableArray.pushMap(mapToWritableMap((Map) value));
      } else if (value instanceof List) {
        writableArray.pushArray(listToWritableArray((List) value));
      }
    }

    return writableArray;
  }


  static public WritableMap mapToWritableMap(Map<String, Object> map) {
    WritableMap writableMap = new WritableNativeMap();

    for (String key : map.keySet()) {
      Object object = map.get(key);

      if (object instanceof Map) {
        writableMap.putMap(key, mapToWritableMap((Map) object));
      } else if (object instanceof List) {
        writableMap.putArray(key, listToWritableArray((List) object));
      } else if (object instanceof String) {
        writableMap.putString(key, (String) object);
      } else if (object instanceof Boolean) {
        writableMap.putBoolean(key, (Boolean) object);
      } else if (object instanceof Float || object instanceof Double) {
        writableMap.putDouble(key, (Double) object);
      } else if (object instanceof Integer) {
        writableMap.putInt(key, (Integer) object);
      }
    }

    return writableMap;
  }

  static public int getExifAngle(String filePath) {
    int angle = 0;
    try {
      ExifInterface exif = new ExifInterface(filePath);
      int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          angle = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          angle = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          angle = 270;
          break;
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return angle;
  }


  public static String getPath(Context context, Uri uri) throws URISyntaxException {
    if ("content".equalsIgnoreCase(uri.getScheme())) {
      String[] projection = { "data"};
      Cursor cursor = null;

      try {
        context.getContentResolver().takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        cursor = context.getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow( "_data");
        if (cursor.moveToFirst()) {
          // Method returns here with null value
          return cursor.getString(column_index);
        }
      } catch (Exception e) {
        // Eat it
        throw e;
      }
    }
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

}
