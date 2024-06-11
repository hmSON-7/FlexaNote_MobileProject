package com.cookandroid.project_flexanote;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFolderClickListener {

    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_CODE_OPEN_FILE = 101;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 102;
    private static final String TAG = "MainActivity";

    RelativeLayout baseLayout;
    RecyclerView recyclerView;
    FileAdapter fileAdapter;
    List<File> fileList;
    private File currentDirectory;
    private Toolbar toolbar;
    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseLayout = findViewById(R.id.baseLayout);
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("My Files");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
            } else {
                initialize();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                initialize();
            }
        }
    }

    private void initialize() {
        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(fileList, this, this);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(fileAdapter);

        currentDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FlexaNote");
        loadFiles(currentDirectory);
    }

    private void loadFiles(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File[] files = directory.listFiles();
        if (files != null) {
            fileList.clear();
            fileList.addAll(Arrays.asList(files));
            // 폴더를 파일 앞에 위치하게 정렬
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    if (o1.isDirectory() && !o2.isDirectory()) {
                        return -1;
                    } else if (!o1.isDirectory() && o2.isDirectory()) {
                        return 1;
                    } else {
                        return o1.getName().compareTo(o2.getName());
                    }
                }
            });
            fileAdapter.notifyDataSetChanged();
        }
        setTitle(directory.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu1, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (isSelectionMode) {
            getMenuInflater().inflate(R.menu.menu_selection_mode, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu1, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (isSelectionMode) {
            if (itemId == R.id.delete_selected_files) {
                deleteSelectedFiles();
                return true;
            } else if (itemId == R.id.cancel_selection_mode) {
                cancelSelectionMode();
                return true;
            }
        } else {
            if (itemId == R.id.add_file) {
                openFilePicker();
                return true;
            } else if (itemId == R.id.add_folder) {
                addFolder();
                return true;
            } else if (itemId == R.id.selection_mode) {
                enterSelectionMode();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType != null && (mimeType.equals("application/msword") || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                        // 워드 파일을 PDF로 변환
                        new ConvertWordToPdfTask(uri).execute();
                    } else {
                        // PDF 파일 복사
                        copyFileToFlexaNote(uri);
                    }
                }
            }
        } else if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initialize();
                } else {
                    showToast("파일 접근 권한이 필요합니다.");
                }
            }
        }
    }

    private void copyFileToFlexaNote(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        String fileName = getFileName(uri);

        if (fileName != null) {
            File destFile = new File(currentDirectory, fileName);
            try (InputStream inputStream = contentResolver.openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(destFile)) {
                if (inputStream != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    showToast("파일이 성공적으로 복사되었습니다.");
                    loadFiles(currentDirectory); // 파일 목록 새로고침
                }
            } catch (IOException e) {
                showToast("파일 복사에 실패했습니다: " + e.getMessage());
            }
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void deleteSelectedFiles() {
        fileAdapter.deleteSelectedFiles();
        cancelSelectionMode();
        loadFiles(currentDirectory); // 파일 목록 새로고침
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        fileAdapter.setSelectionMode(true);
        invalidateOptionsMenu();
    }

    private void cancelSelectionMode() {
        isSelectionMode = false;
        fileAdapter.setSelectionMode(false);
        invalidateOptionsMenu();
    }

    private void addFolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 폴더 추가");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("생성", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String folderName = input.getText().toString();
                File newFolder = new File(currentDirectory, folderName);
                if (newFolder.mkdir()) {
                    showToast("폴더가 생성되었습니다.");
                    loadFiles(currentDirectory);
                } else {
                    showToast("폴더 생성에 실패했습니다.");
                }
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initialize();
            } else {
                showToast("권한이 거부되었습니다.");
            }
        }
    }

    @Override
    public void onFolderClick(File folder) {
        currentDirectory = folder;
        loadFiles(currentDirectory);
        setTitle(folder.getName());
    }

    @Override
    public void onFileClick(File file) {

    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            cancelSelectionMode();
        } else if (!currentDirectory.equals(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/FlexaNote"))) {
            currentDirectory = currentDirectory.getParentFile();
            loadFiles(currentDirectory);
            setTitle(currentDirectory.getName());
        } else {
            super.onBackPressed();
        }
    }

    private class ConvertWordToPdfTask extends AsyncTask<Void, Void, Boolean> {
        private Uri fileUri;
        private String pdfFilePath;

        public ConvertWordToPdfTask(Uri fileUri) {
            this.fileUri = fileUri;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String apiKey = "your_api_key";  // ConvertAPI API 키
                String endpoint = "https://v2.convertapi.com/convert/docx/to/pdf?Secret=" + apiKey;
                String fileName = getFileName(fileUri);
                File inputFile = new File(getCacheDir(), fileName);

                // 파일을 캐시 디렉토리로 복사
                try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                     OutputStream outputStream = new FileOutputStream(inputFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }

                Log.d(TAG, "File copied to cache: " + inputFile.getAbsolutePath());

                // 파일 업로드 및 변환 요청
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String boundary = "*****";
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(("--" + boundary + "\r\n").getBytes());
                    outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + inputFile.getName() + "\"\r\n").getBytes());
                    outputStream.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes());

                    try (InputStream inputStream = new FileInputStream(inputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }

                    outputStream.write(("\r\n--" + boundary + "--\r\n").getBytes());
                }

                // 응답 처리
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == 200) {
                    try (InputStream responseStream = connection.getInputStream()) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = responseStream.read(buffer)) > 0) {
                            byteArrayOutputStream.write(buffer, 0, length);
                        }
                        String response = byteArrayOutputStream.toString("UTF-8");
                        Log.d(TAG, "Response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        String fileData = jsonResponse.getJSONArray("Files").getJSONObject(0).getString("FileData");

                        // PDF 파일 저장
                        byte[] pdfBytes = Base64.decode(fileData, Base64.DEFAULT);
                        File pdfFile = new File(currentDirectory, fileName.replaceAll("\\.[^.]+$", ".pdf"));
                        pdfFilePath = pdfFile.getAbsolutePath();
                        try (OutputStream outputStream = new FileOutputStream(pdfFile)) {
                            outputStream.write(pdfBytes);
                        }

                        Log.d(TAG, "PDF File saved: " + pdfFilePath);

                        return true;
                    }
                } else {
                    try (InputStream errorStream = connection.getErrorStream()) {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = errorStream.read(buffer)) > 0) {
                            byteArrayOutputStream.write(buffer, 0, length);
                        }
                        String errorResponse = byteArrayOutputStream.toString("UTF-8");
                        Log.e(TAG, "Error Response: " + errorResponse);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception: " + e.getMessage());
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                showToast("파일이 PDF로 성공적으로 변환되었습니다.");
                loadFiles(currentDirectory);
            } else {
                showToast("파일 변환에 실패했습니다.");
            }
        }
    }

}