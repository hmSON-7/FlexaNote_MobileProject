package com.cookandroid.project_flexanote;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<File> fileList;
    private List<File> selectedFiles;
    private Context context;
    private OnFolderClickListener onFolderClickListener;
    private boolean isSelectionMode = false;

    public FileAdapter(List<File> fileList, Context context, OnFolderClickListener onFolderClickListener) {
        this.fileList = fileList;
        this.context = context;
        this.onFolderClickListener = onFolderClickListener;
        this.selectedFiles = new ArrayList<>();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.fileNameTextView.setText(file.getName());
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(file);
            } else {
                if (file.isDirectory()) {
                    onFolderClickListener.onFolderClick(file);
                } else {
                    openFile(file);
                }
            }
        });

        if (file.isDirectory()) {
            holder.fileIconImageView.setImageResource(R.drawable.folder_icon);
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            if ("pdf".equalsIgnoreCase(extension)) {
                holder.fileIconImageView.setImageResource(R.drawable.pdf_icon);
            } else {
                holder.fileIconImageView.setImageResource(R.drawable.ic_launcher_background); // 기본 파일 아이콘 설정
            }
        }

        holder.itemView.setBackgroundColor(selectedFiles.contains(file) ? 0x9934B5E4 : 0x00000000);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean isSelectionMode) {
        this.isSelectionMode = isSelectionMode;
        selectedFiles.clear();
        notifyDataSetChanged();
    }

    public void deleteSelectedFiles() {
        List<File> filesToDelete = new ArrayList<>(selectedFiles);
        for (File file : filesToDelete) {
            file.delete();
        }
        fileList.removeAll(filesToDelete);
        selectedFiles.clear();
        notifyDataSetChanged();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIconImageView;
        TextView fileNameTextView;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIconImageView = itemView.findViewById(R.id.fileIconImageView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
        }
    }

    private void openFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, getMimeType(fileUri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "파일을 열 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return mimeType;
    }

    public interface OnFolderClickListener {
        void onFolderClick(File folder);

        void onFileClick(File file);
    }
}