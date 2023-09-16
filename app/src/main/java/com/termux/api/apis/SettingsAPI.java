package com.termux.api.apis;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsAPI {

    final public static class Namespace {
        final static String SYSTEM = "system";
        final static String SECURE = "secure";
        final static String GLOBAL = "global";
        static String getNamespace(String namespace) {
            if (namespace == null) return null;
            switch (namespace.toLowerCase(Locale.ENGLISH)) {
                case SYSTEM: return SYSTEM;
                case SECURE: return SECURE;
                case GLOBAL: return GLOBAL;
                default: return null;
            }
        }
    }

    final public static class Method {
        final static String LIST = "list";
        final static String GET = "get";
        final static String PUT = "put";
        final static String DELETE = "delete";
        static String getMethod(String method) {
            if (method == null) return null;
            switch (method.toLowerCase(Locale.ENGLISH)) {
                case LIST: return LIST;
                case GET: return GET;
                case PUT: return PUT;
                case DELETE: return DELETE;
                default: return null;
            }
        }
    }

    private static final String LOG_TAG = "SettingsAPI";

    public static void onReceive(final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");
        final ContentResolver resolver = context.getContentResolver();

        final String method = Method.getMethod(intent.getStringExtra("method"));
        final String namespace = Namespace.getNamespace(intent.getStringExtra("namespace"));

        ResultReturner.returnData(context, intent, (out) -> {
            if (namespace == null) {
                out.println("ERR:Invalid Namespace");
                return;
            }
            if (method == null) {
                out.println("ERR:Invalid Method");
                return;
            }
            if (method.equals(Method.LIST)) {
                List<String> lines = getList(resolver, namespace);
                for (String line : lines) out.println(line);
                return;
            }
            if (method.equals(Method.GET)) {
                final String key = intent.getStringExtra("key");
                if (key == null) {
                    out.println("ERR:KeyName is Null");
                    return;
                }
                out.println(getValue(resolver, namespace, key));
                return;
            }
            if (method.equals(Method.DELETE)) {
                final String key = intent.getStringExtra("key");
                if (key == null){
                    out.println("ERR:KeyName is Null");
                    return;
                }
                try {
                    out.println(deleteValue(resolver, namespace, key));
                } catch (SecurityException e) {
                    String message = e.getMessage();
                    if (message == null) {
                        out.println("ERR:Unknown Security Exception");
                        return;
                    }
                    if (message.contains("WRITE_SETTINGS")) {
                        out.println("ERR:android.permission.WRITE_SETTINGS required");
                        return;
                    }
                    if (message.contains("WRITE_SECURE_SETTINGS")) {
                        out.println("ERR:android.permission.WRITE_SECURE_SETTINGS required");
                    }
                }
            }
            if (method.equals(Method.PUT)) {
                final String key = intent.getStringExtra("key");
                final String value = intent.getStringExtra("value");
                if (key == null) {
                    out.println("ERR:KeyName is Null");
                    return;
                }
                if (value == null) {
                    out.println("ERR:KeyValue is Null");
                    return;
                }
                try {
                    if (putValue(resolver, namespace, key, value)) {
                        out.println("Success");
                    } else {
                        out.println("Failure (No Exception)");
                    }
                } catch (SecurityException e) {
                    String message = e.getMessage();
                    if (message == null) {
                        out.println("ERR:Unknown Security Exception");
                        return;
                    }
                    if (message.contains("WRITE_SETTINGS")) {
                        out.println("ERR:android.permission.WRITE_SETTINGS required");
                        return;
                    }
                    if (message.contains("WRITE_SECURE_SETTINGS")) {
                        out.println("ERR:android.permission.WRITE_SECURE_SETTINGS required");
                    }
                }
            }
        });
    }
    public static String deleteValue(ContentResolver resolver, String namespace, String keyname) {
        Uri uri;
        switch (Namespace.getNamespace(namespace)) {
            case Namespace.SYSTEM:
                uri = Settings.System.CONTENT_URI;
                break;
            case Namespace.SECURE:
                uri = Settings.Secure.CONTENT_URI;
                break;
            case Namespace.GLOBAL:
                uri = Settings.Global.CONTENT_URI;
                break;
            default:
                return "ERR:Invalid Namespace";
        }
        try (Cursor cursor = resolver.query(uri, new String[]{"name"}, "name = ?", new String[]{keyname}, null)) {
            if ((cursor != null) && (cursor.getCount() == 1) && cursor.moveToFirst()) {
                return "Deleted " + resolver.delete(uri, "name = ?", new String[]{cursor.getString(0)}) + " row(s)";
            } else {
                return "Deleted 0 row(s)";
            }
        }
    }
    public static String getValue(ContentResolver resolver, String namespace, String keyname) {
        switch (Namespace.getNamespace(namespace)) {
            case Namespace.SYSTEM:
                return Settings.System.getString(resolver, keyname);
            case Namespace.SECURE:
                return Settings.Secure.getString(resolver, keyname);
            case Namespace.GLOBAL:
                return Settings.Global.getString(resolver, keyname);
            default:
                return "ERR:Invalid Namespace";
        }
    }
    public static List<String> getList(ContentResolver resolver, String table) {
        final ArrayList<String> lines = new ArrayList<>();
        Uri uri;
        switch (Namespace.getNamespace(table)) {
            case Namespace.SYSTEM:
                uri = Settings.System.CONTENT_URI;
                break;
            case Namespace.SECURE:
                uri = Settings.Secure.CONTENT_URI;
                break;
            case Namespace.GLOBAL:
                uri = Settings.Global.CONTENT_URI;
                break;
            default:
                lines.add("Invalid Namespace");
                return lines;
        }
        try (Cursor cursor = resolver.query(uri, new String[]{"name", "value"}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                lines.add(cursor.getString(0) + "=" + cursor.getString(1));
            }
        }
        lines.sort(String::compareToIgnoreCase);
        return lines;
    }
    public static Boolean putValue(ContentResolver resolver, String namespace, String key, String value) {
        switch (Namespace.getNamespace(namespace)) {
            case Namespace.SYSTEM:
                return Settings.System.putString(resolver, key, value);
            case Namespace.SECURE:
                return  Settings.Secure.putString(resolver, key, value);
            case Namespace.GLOBAL:
                return  Settings.Global.putString(resolver, key, value);
        }
        return false;
    }
}
