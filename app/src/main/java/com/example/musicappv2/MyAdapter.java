package com.example.musicappv2;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<String> items;
    Context context;

    MyAdapter(Context context)
    {
        this.context = context;
    }
    private OnItemClickListener onItemClickListener;
    private StorageReference storageReference;
    public void setItems(List<String> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void clear() {
        items = null;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(String itemName);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview2, parent, false);
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child("AllSongs");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String itemName = items.get(position);
        holder.bind(itemName);
        holder.favoriteee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transferToFavorite(itemName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imageView;
        TextView textView;
        CardView cardView;
        ImageButton favoriteee;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView = itemView.findViewById(R.id.textView);
            cardView = itemView.findViewById(R.id.cardViewUpdate);
            favoriteee = itemView.findViewById(R.id.favorite);
            itemView.setOnClickListener(this);
        }

        public void bind(String itemName) {
            textView.setText(itemName);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                String itemName = items.get(position);
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(itemName);
                }
            }
        }
    }

    private void transferToFavorite(String itemName) {
        // Assuming you have a reference to the FirebaseStorage instance for the user's favorite songs
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        StorageReference userFavoriteReference = FirebaseStorage.getInstance().getReference().child("Songs/" + userEmail + "/");

        // Copy the song file from "AllSongs" to "UserFavorites"
        StorageReference sourceReference = storageReference.child(itemName);
        File localFile = null; // Temporary local file to store the downloaded song

        try {
            localFile = File.createTempFile("temp_song", "mp3"); // Create a temporary file
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (localFile != null) {
            File finalLocalFile = localFile;
            Toast.makeText(context, "Adding to Favourite...", Toast.LENGTH_SHORT).show();
            sourceReference.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Uri localFileUri = Uri.fromFile(finalLocalFile);

                            StorageReference newFileReference = userFavoriteReference.child(itemName);
                            newFileReference.putFile(localFileUri)
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            finalLocalFile.delete();
                                            Toast.makeText(context, "Track added", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            finalLocalFile.delete();
                                            Toast.makeText(context, "Failed to add track", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to download the song from the source reference
                            // Handle the error appropriately
                        }
                    });
        }
    }
}
