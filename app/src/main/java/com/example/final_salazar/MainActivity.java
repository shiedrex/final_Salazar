package com.example.final_salazar;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ImageView addImage;
    EditText id, name, desc;
    Button saveBtn, searchBtn, updateBtn, deleteBtn, logoutBtn, clearBtn;
    TextView addimagereq;

    StorageReference storageReference;
    Uri imageUri;

    FirebaseAuth firebaseAuth;
    FirebaseUser currentUser;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;
    ProgressDialog pd;
    Records records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference("heroes");
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        addImage = findViewById(R.id.addImage);
        id = findViewById(R.id.id);
        name = findViewById(R.id.name);
        desc = findViewById(R.id.desc);
        saveBtn = findViewById(R.id.saveButton);
        searchBtn = findViewById(R.id.searchButton);
        updateBtn = findViewById(R.id.updateButton);
        deleteBtn = findViewById(R.id.deleteButton);
        logoutBtn = findViewById(R.id.logoutButton);
        clearBtn = findViewById(R.id.clearButton);

        addimagereq = findViewById(R.id.addimagerequired);

        pd = new ProgressDialog(this);
        records = new Records();

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            addImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(openGalleryIntent, 1000);
                }
            });
        }

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ID = id.getText().toString();
                String NAME = name.getText().toString();
                String DESC = desc.getText().toString();

                if (ID.isEmpty()) {
                    id.setError("Hero Id is required");
                    id.requestFocus();
                    return;
                }
                if (NAME.isEmpty()) {
                    name.setError("Hero Name is required");
                    name.requestFocus();
                    return;
                }
                if (DESC.isEmpty()) {
                    desc.setError("Hero Description is required");
                    desc.requestFocus();
                    return;
                }

                if (imageUri == null && addImage.getDrawable() == null) {
                    addimagereq.setVisibility(View.VISIBLE);
                    addimagereq.requestFocus();
                    return;
                } else {
                    addimagereq.setVisibility(View.GONE);
                }

                Query query = reference.child(firebaseAuth.getCurrentUser().getUid()).orderByChild("id").equalTo(ID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getChildrenCount() > 0) {
                            pd.dismiss();
                            id.setError("Hero Id already exists");
                            id.requestFocus();
                            Toast.makeText(MainActivity.this, "Hero Id already exists", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            uploadToFirebase();
                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pd.dismiss();
                    }
                });
                pd.show();
                pd.setMessage("Saving...Please Wait");

            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ID = id.getText().toString();

                Query query = reference.child(firebaseAuth.getCurrentUser().getUid()).orderByChild("id").equalTo(ID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            pd.dismiss();
                            id.setError("Hero Id does not exist");
                            id.requestFocus();

                            name.setText("");
                            desc.setText("");
                            Glide.with(getApplicationContext()).load(R.drawable.ic_launcher_background).into(addImage);

                            Toast.makeText(MainActivity.this, "Hero Id does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            for (DataSnapshot dataSnapshot1 : snapshot.getChildren()) {
                                String NAME = "" + dataSnapshot1.child("name").getValue();
                                String DESC = "" + dataSnapshot1.child("desc").getValue();
                                String IMAGE = "" + dataSnapshot1.child("image").getValue();

                                pd.dismiss();
                                name.setText(NAME);
                                desc.setText(DESC);

                                id.setError(null);
                                name.setError(null);
                                desc.setError(null);

                                try {
                                    Glide.with(getApplicationContext()).load(IMAGE).placeholder(R.drawable.ic_launcher_background).into(addImage);
                                } catch (Exception e) {

                                }
                                Toast.makeText(MainActivity.this, "Hero Id found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pd.dismiss();
                    }
                });
                pd.show();
                pd.setMessage("Searching...Please Wait");
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ID = id.getText().toString();
                String NAME = name.getText().toString();
                String DESC = desc.getText().toString();

                if (ID.isEmpty()) {
                    id.setError("Hero Id is required");
                    id.requestFocus();
                    return;
                }
                if (NAME.isEmpty()) {
                    name.setError("Hero Name is required");
                    name.requestFocus();
                    return;
                }
                if (DESC.isEmpty()) {
                    desc.setError("Hero Description is required");
                    desc.requestFocus();
                    return;
                }

                if (imageUri == null) {
                    addimagereq.setVisibility(View.VISIBLE);
                    addimagereq.requestFocus();
                    return;
                } else {
                    addimagereq.setVisibility(View.GONE);
                }

                Query query = reference.child(firebaseAuth.getCurrentUser().getUid()).orderByChild("id").equalTo(ID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            pd.dismiss();
                            id.setError("Hero Id does not exist");
                            id.requestFocus();
                            name.setText("");
                            desc.setText("");
                            Glide.with(getApplicationContext()).load(R.drawable.ic_launcher_background).into(addImage);

                            Toast.makeText(MainActivity.this, "Hero Id does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            if (imageUri != null) {
                                final StorageReference fileRef = storageReference.child("heroes/" + firebaseAuth.getCurrentUser().getUid() + id.getText().toString() + ".jpg");
                                fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                pd.dismiss();
                                                String IMAGE = uri.toString();

                                                HashMap prod = new HashMap();
                                                prod.put("name", NAME);
                                                prod.put("desc", DESC);
                                                prod.put("image", IMAGE);

                                                id.setError(null);
                                                name.setError(null);
                                                desc.setError(null);

                                                reference.child(firebaseAuth.getCurrentUser().getUid()).child(ID).updateChildren(prod);
                                                Toast.makeText(MainActivity.this, "Record updated", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Failed.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pd.dismiss();
                    }
                });
                pd.show();
                pd.setMessage("Updating...Please Wait");
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ID = id.getText().toString();

                Query query = reference.child(firebaseAuth.getCurrentUser().getUid()).orderByChild("id").equalTo(ID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            pd.dismiss();

                            id.setError("Hero Id does not exist");
                            id.requestFocus();
                            name.setText("");
                            desc.setText("");
                            Glide.with(getApplicationContext()).load(R.drawable.ic_launcher_background).into(addImage);

                            Toast.makeText(MainActivity.this, "Hero Id does not exist", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            storageReference.child("heroes/" + firebaseAuth.getCurrentUser().getUid() + ID + ".jpg").delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    pd.dismiss();
                                                    id.setText("");
                                                    id.setError(null);
                                                    name.setText("");
                                                    name.setError(null);
                                                    desc.setText("");
                                                    desc.setError(null);
                                                    Glide.with(getApplicationContext()).load(R.drawable.ic_launcher_background).into(addImage);

                                                    reference.child(firebaseAuth.getCurrentUser().getUid()).child(ID).removeValue();
                                                    Toast.makeText(MainActivity.this, "Record Deleted", Toast.LENGTH_SHORT).show();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Record Failed to Delete", Toast.LENGTH_SHORT).show();
                                            pd.dismiss();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        pd.dismiss();
                    }
                });
                pd.show();
                pd.setMessage("Deleting...Please Wait");
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                id.setText("");
                id.setError(null);
                name.setText("");
                name.setError(null);
                desc.setText("");
                desc.setError(null);
                Glide.with(getApplicationContext()).load(R.drawable.ic_launcher_background).into(addImage);
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Logout");
                builder.setMessage("Are you sure you want to logout?");
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        finish();
                        startActivity(new Intent(MainActivity.this, Login.class));
                    }
                });
                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                imageUri = data.getData();
                addImage.setImageURI(imageUri);
                addimagereq.setVisibility(View.GONE);
            } else {
                addimagereq.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean uploadToFirebase() {
        if (imageUri != null) {
            final StorageReference fileRef = storageReference.child("heroes/" + firebaseAuth.getCurrentUser().getUid() + id.getText().toString() + ".jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            records.setImage(uri.toString());
                            records.setId(id.getText().toString());
                            records.setName(name.getText().toString());
                            records.setDesc(desc.getText().toString());
                            reference.child(firebaseAuth.getCurrentUser().getUid()).child(id.getText().toString()).setValue(records);

                            id.setText("");
                            id.setError(null);
                            name.setText("");
                            name.setError(null);
                            desc.setText("");
                            desc.setError(null);
                            Glide.with(getApplicationContext()).load(R.drawable.ic_launcher_background).into(addImage);

                            Toast.makeText(MainActivity.this, "Record Added", Toast.LENGTH_SHORT).show();
                            pd.dismiss();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Failed.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            pd.dismiss();
            Toast.makeText(MainActivity.this, "Failed.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}