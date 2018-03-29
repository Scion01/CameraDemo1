package scion_01.camerademo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kosalgeek.android.photoutil.CameraPhoto;
import com.kosalgeek.android.photoutil.GalleryPhoto;
import com.kosalgeek.android.photoutil.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView imageView;
    private Button button;
    //add firebase storage dependency in build.gradle

    private StorageReference mStorageRef;
    CameraPhoto cameraPhoto;
    GalleryPhoto galleryPhoto;

    final int CAMERA_REQUEST = 13323;
    final int GALLERY_REQUEST = 22131;
    public  static final int PERMISSIONS_MULTIPLE_REQUEST = 123;



    private Uri uri;
    private Button uploadButton;
    private Button openGallery;
    private Bitmap bitmap;
    private ExifInterface exif;
    private LocationManager locationManager;
    private String photoPath;
    private String longitude, latitude;
    private File file;
    LocationListener locationListener;
    private String uploadedFileName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mStorageRef = FirebaseStorage.getInstance().getReference();
        //gets root ref

        imageView = (ImageView) findViewById(R.id.imageFromCamera);
        button = (Button) findViewById(R.id.buttonToClick);
        uploadButton = (Button) findViewById(R.id.buttonForUpload);
        openGallery = (Button) findViewById(R.id.buttonForGallery);



        cameraPhoto = new CameraPhoto(getApplicationContext());
        galleryPhoto = new GalleryPhoto(getApplicationContext());

        button.setOnClickListener(this);
        openGallery.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);

        locationListener = new MyLocationListener();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        //this above ignores URI exposure

        checkAndroidVersion();

    }

    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();

        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

    }
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA)
            +ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (this, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                Snackbar.make(this.findViewById(android.R.id.content),
                        "Please Grant Permissions to upload photos",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(
                                            new String[]{Manifest.permission
                                                    .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                                            PERMISSIONS_MULTIPLE_REQUEST);
                                }
                            }
                        }).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission
                                    .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_MULTIPLE_REQUEST);
                }
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean fineLocation = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if(cameraPermission && readExternalFile && fineLocation)
                    {
                        Toast.makeText(this,"All the required permissions are granted!!",Toast.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(this.findViewById(android.R.id.content),
                                "Please Grant Permissions to upload photos",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(
                                                    new String[]{Manifest.permission
                                                            .READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                                                    PERMISSIONS_MULTIPLE_REQUEST);
                                        }
                                    }
                                }).show();
                    }
                }
                break;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == CAMERA_REQUEST){
                photoPath  = cameraPhoto.getPhotoPath();
                Log.d("photoPath",photoPath);
                try {
                    bitmap = ImageLoader.init().from(photoPath).requestSize(512,512).getBitmap();
                    imageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "Something Wrong while loading photos", Toast.LENGTH_SHORT).show();
                }
            }
            else if(requestCode == GALLERY_REQUEST){
                uri = data.getData();
                galleryPhoto.setPhotoUri(uri);
                photoPath = galleryPhoto.getPath();
                try {
                    bitmap = ImageLoader.init().from(photoPath).requestSize(512,512).getBitmap();
                    imageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "Something Wrong while loading photos", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        if(v == button){
            uploadButton.setVisibility(View.VISIBLE);
            try {
                startActivityForResult(cameraPhoto.takePhotoIntent(),CAMERA_REQUEST);
                cameraPhoto.addToGallery();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"Something went wrong!!",Toast.LENGTH_SHORT).show();
            }
        } else if (v == openGallery) {
            uploadButton.setVisibility(View.VISIBLE);
            startActivityForResult(galleryPhoto.openGalleryIntent(),GALLERY_REQUEST);
        }
        else if(v ==uploadButton){

            try {
                ExifInterface exif = new ExifInterface(photoPath);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitude);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitude);
                exif.saveAttributes();
                Log.i("LATITUDE: ", latitude);
                Log.i("LONGITUDE: ", longitude);
                Toast.makeText(this,"Metadata Attached!",Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Log.i("not able to attach meta","true");
                Toast.makeText(this,"Metadata Attachment Failed!",Toast.LENGTH_SHORT).show();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);



            byte[] dataBAOS = baos.toByteArray();


            uploadedFileName="ImageFromUser"+new Date();
            StorageReference imagesRef = mStorageRef.child(uploadedFileName);

            final UploadTask uploadTask = imagesRef.putBytes(dataBAOS);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(),"Sending failed", Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(getApplicationContext(),"Image Uploaded!!", Toast.LENGTH_SHORT).show();

                    StorageReference childRef = mStorageRef.child(uploadedFileName);
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setContentType("image/png")
                            .setCustomMetadata("Longitude", longitude)
                            .setCustomMetadata("Latitude",latitude)
                            .build();
                    childRef.updateMetadata(metadata)
                            .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {
                                    Toast.makeText(getApplicationContext(),"Metadata Updated!!",Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Toast.makeText(getApplicationContext(),"Metadata Failed!!",Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });

        }

    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            Toast.makeText(getBaseContext(), "Location changed: Lat: " + loc.getLatitude() + " Lng: "
                            + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            longitude =  Double.toString(loc.getLongitude());
            Log.v("Long", longitude);
            latitude = Double.toString(loc.getLatitude());
            Log.v("Lat", latitude);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
            //Toast.makeText(getApplicationContext(),"Make sure that GPS is ON",Toast.LENGTH_SHORT).show();
            Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionsIntent);
        }
    }
}
