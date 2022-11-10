package com.migrate.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.iq80.leveldb.*;
import static org.fusesource.leveldbjni.JniDBFactory.*;
import java.io.*;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaWebView;
import java.io.File;

/**
 * Main class that is instantiated by cordova
 * Acts as a "bridge" between the SDK and the cordova layer
 * 
 * This plugin migrates WebSQL and localStorage from the old webview to the new webview
 * 
 * TODO
 * - Test if we can we remove old file:// keys?
 * - Properly handle exceptions? We have a catch-all at the moment that is dealt with in the `initialize` function
 * - migrating IndexedDB (may not be possible because of leveldb complexities)
 */
public class MigrateStorage extends CordovaPlugin {
    // Switch this value to enable debug mode
    private static final boolean DEBUG_MODE = true;

    private static final String TAG = "com.migrate.android";
    private static final String FILE_PROTOCOL = "file://";
    private static final String WEBSQL_FILE_DIR_NAME = "file__0";
    private static final String DEFAULT_PORT_NUMBER = "8080";
    private static final String CDV_SETTING_PORT_NUMBER = "WKPort";

    private String portNumber;


    private void logDebug(String message) {
        if(DEBUG_MODE) Log.d(TAG, message);
    }

    private String getLocalHostProtocolDirName() {
        return "https_localhost_" + this.portNumber;
    }

    private String getLocalHostProtocol() {
        return "https://mobilezuz:" + this.portNumber;
    }

    private String getRootPath() {
        Context context = cordova.getActivity().getApplicationContext();
        try (Stream<Path> stream = Files.walk(Paths.get(context.getFilesDir().getAbsolutePath()))) {
            stream.filter(Files::isRegularFile)
            .forEach(System.out::println);
        }
        return context.getFilesDir().getAbsolutePath().replaceAll("/files", "");
    }

    private String getWebViewRootPath() {
        return this.getRootPath() + "/app_webview";
    }

    private String getLocalStorageRootPath() {
        return this.getWebViewRootPath() + "/Local Storage";
    }

    private String getWebSQLDatabasesPath() {
        return this.getWebViewRootPath() + "/databases";
    }

    private String getWebSQLReferenceDbPath() {
        return this.getWebSQLDatabasesPath() + "/Databases.db";
    }

    /**
     * Migrate localStorage from `file://` to `http://localhost:{portNumber}`
     *
     * TODO Test if we can we remove old file:// keys?
     *
     * @throws Exception - Can throw LevelDBException
     */
    private void migrateLocalStorage() throws Exception {
        this.logDebug("migrateLocalStorage: Migrating localStorage..");

        String levelDbPath = this.getLocalStorageRootPath() + "/leveldb";
        this.logDebug("migrateLocalStorage: levelDbPath: " + levelDbPath);

        File levelDbDir = new File(levelDbPath);
        if(!levelDbDir.isDirectory() || !levelDbDir.exists()) {
            this.logDebug("migrateLocalStorage: '" + levelDbPath + "' is not a directory or was not found; Exiting");
            return;
        }
        
        Options options = new Options();
        options.createIfMissing(true);
        DB db = factory.open(new File("migration"), options);
        try {
          db.put(bytes("Tampa"), bytes("rocks"));
        } finally {
          // Make sure you close the db to shutdown the 
          // database and avoid resource leaks.
          db.close();
        }

//         LevelDB db = new LevelDB(levelDbPath);

//         String localHostProtocol = this.getLocalHostProtocol();

//         if(db.exists(Utils.stringToBytes("META:" + localHostProtocol))) {
//             this.logDebug("migrateLocalStorage: Found 'META:" + localHostProtocol + "' key; Skipping migration");
//             db.close();
//             return;
//         }

//         // Yes, there is a typo here; `newInterator` 😔
//         LevelIterator iterator = db.newInterator();

//         // To update in bulk!
//         WriteBatch batch = new WriteBatch();


//         // 🔃 Loop through the keys and replace `file://` with `http://localhost:{portNumber}`
//         logDebug("migrateLocalStorage: Starting replacements;");
//         for(iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
//             String key = Utils.bytesToString(iterator.key());
//             byte[] value = iterator.value();

//             if (key.contains(FILE_PROTOCOL)) {
//                 String newKey = key.replace(FILE_PROTOCOL, localHostProtocol);

//                 logDebug("migrateLocalStorage: Changing key:" + key + " to '" + newKey + "'");

//                 // Add new key to db
//                 batch.putBytes(Utils.stringToBytes(newKey), value);
//             } else {
//                 logDebug("migrateLocalStorage: Skipping key:" + key);
//             }
//         }

//         // Commit batch to DB
//         db.write(batch);

//         iterator.close();
//         db.close();

//         this.logDebug("migrateLocalStorage: Successfully migrated localStorage..");
    }

    /**
     * Sets up the plugin interface
     *
     * @param cordova - cdvInterface that contains cordova goodies
     * @param webView - the webview that we're running
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        try {
            super.initialize(cordova, webView);

            this.portNumber = this.preferences.getString(CDV_SETTING_PORT_NUMBER, "");
            if(this.portNumber.isEmpty() || this.portNumber == null) this.portNumber = DEFAULT_PORT_NUMBER;

            logDebug("Starting migration;");

            this.migrateLocalStorage();

            logDebug("Migration completed;");
        } catch (Exception ex) {
            logDebug("Migration filed due to error: " + ex.getMessage());
        }
    }
}
