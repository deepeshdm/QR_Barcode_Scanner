package com.deepesh.qrbarcodescanner;

import static android.content.ContentValues.TAG;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    final static int SELECT_PICTURE_CODE = 101;
    PreviewView previewView_main;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    CameraSelector cameraSelector;
    Preview preview;
    Camera mCamera;
    ImageView flash_light_imageview;
    ImageView image_upload_imageview;
    AdView banner_ad;
    Toolbar toolbar;
    ProcessCameraProvider cameraProvider;
    ExecutorService cameraExecutor;
    HashMap<Integer, String> barcode_types;
    static boolean IS_FLASH_ON=false;
    private final int CAMERA_REQUEST_CODE = 101;

    // Coordinates of rectangular box,these will be
    // used to crop the scanning region.
    public static int LEFT, TOP, RIGHT, BOTTOM;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flash_light_imageview = findViewById(R.id.flash_light_imageView);
        flash_light_imageview.setOnClickListener(v -> {

            flash_light_switch();

        });

        image_upload_imageview = findViewById(R.id.image_upload_imageView);
        image_upload_imageview.setOnClickListener(v -> {
            upload_barcode_image();
        });


        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);


        // contains all barcode types supported
        barcode_types = new HashMap<>();
        barcode_types.put(4096, "AZTEC");
        barcode_types.put(2048, "PDF417");
        barcode_types.put(1024, "UPC_E");
        barcode_types.put(512, "UPC_A");
        barcode_types.put(256, "QR_CODE");
        barcode_types.put(128, "ITF");
        barcode_types.put(64, "EAN_8");
        barcode_types.put(32, "EAN_13");
        barcode_types.put(16, "DATA_MATRIX");
        barcode_types.put(0, "UNKNOWN");
        barcode_types.put(1, "CONTACT_INFO");
        barcode_types.put(2, "EMAIL");
        barcode_types.put(3, "ISBN");
        barcode_types.put(4, "PHONE");
        barcode_types.put(5, "PRODUCT");
        barcode_types.put(6, "SMS");
        barcode_types.put(7, "TEXT");
        barcode_types.put(8, "URL");
        barcode_types.put(9, "WIFI");
        barcode_types.put(10, "GEO");
        barcode_types.put(11, "CALENDAR_EVENT");
        barcode_types.put(12, "DRIVER_LICENSE");

        banner_ad = findViewById(R.id.Banner_adView);

        previewView_main = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview = new Preview.Builder().build();

        try {
            cameraProvider = cameraProviderFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            // This should never be reached.
        }


        // Initializing ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        banner_ad.loadAd(adRequest);


        ask_permissions();

    }


    //=============================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.license:
                startActivity(new Intent(this, License_Activity.class));
                break;
            case R.id.contact:
                showMailDialog();
                break;

        }


        return super.onOptionsItemSelected(item);
    }


    // triggers when user clicks on 'Mail us' option
    public void showMailDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please write your mail in English.If possible please " +
                "include photos or screenshots if necessary.")
                .setCancelable(false);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String[] to = new String[]{"deepeshmhatre133@gmail.com"};
                String subject = "QR&Barcode Scanner : QUERY/FEEDBACK";

                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL,to);
                email.putExtra(Intent.EXTRA_SUBJECT, subject);
                //need this to prompts email client only
                email.setType("message/rfc822");


                try {
                    //start email intent
                    startActivity(Intent.createChooser(email, "Choose an Email client :"));
                } catch (Exception e) {
                    //if any thing goes wrong for example no email client application or any exception
                    //get and show exception message
                    e.fillInStackTrace();
                }

            }
        });


        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //remove the Dialog box form screen.
                //cancel button
                builder.create().dismiss();

            }
        });

        builder.show();
    }


    //=============================================================================


    // triggers when user clicks on 'Upload Image' option
    // Use can pick an image from gallery for scanning.
    private void upload_barcode_image(){

        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i, "Select Barcode/QR Image"), SELECT_PICTURE_CODE);

    }


    // this function is triggered when user
    // selects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // compare the resultCode with the
            // SELECT_PICTURE_CODE constant
            if (requestCode == SELECT_PICTURE_CODE) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        InputImage inputImage = InputImage.fromBitmap(bitmap,90);
                        scan_barcode(inputImage,true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("TAG2", "ERROR OCCCURED");
                        Log.d("TAG2", e.getMessage());
                    }

                }
            }
        }
    }



    //=============================================================================

    private void load_camera_preview() {

        cameraProviderFuture.addListener(() -> {
            bindPreview(cameraProvider);
        }, ContextCompat.getMainExecutor(this));

        draw_preview_rectangle();

    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {

        preview.setSurfaceProvider(previewView_main.getSurfaceProvider());

        // ImageAnalysis for processing camera preview frames
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(1280, 720))
                        .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

                // preparing input image
                @SuppressLint("UnsafeOptInUsageError")
                Image mediaImage = image.getImage();

                if (mediaImage != null) {

                    // convert Image to Bitmap
                    // Bitmap bmp = ImageToBitmap(mediaImage);

                    // Cropping Bitmap image so that we scan rect box
                    // GETTING AN ERROR HERE !
                    // bmp = Bitmap.createBitmap(bmp,LEFT,TOP,RIGHT,BOTTOM);

                    // convert Bitmap to InputImage since API takes only InputImage type.
                    // InputImage inputImage = InputImage.fromBitmap(bmp, image.getImageInfo().getRotationDegrees());

                    InputImage inputImage = InputImage.fromMediaImage
                            (mediaImage, image.getImageInfo().getRotationDegrees());
                    scan_barcode(inputImage,false);
                }

                image.close();
            }
        });

        mCamera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);

    }


    // Scans input image for barcode and does required actions
    // "is_choosen" defines if the image given is uploaded
    // using 'Image Upload' option in options menu.
    private void scan_barcode(InputImage inputImage,Boolean is_choosen) {

        // setting up barcode scanner api
        BarcodeScanner scanner = BarcodeScanning.getClient();

        scanner.process(inputImage)
                .addOnFailureListener(e -> {

                    Log.d("TAG", "scan_barcode.OnFailureListener : " + "NO BARCODE DETECTED");

                })
                .addOnSuccessListener(barcodes -> {

                    // 'barcodes' is a list of all detected barcodes
                    Log.d("TAG4", "scan_barcode.OnSucessListener : " + barcodes.size());

                    if (barcodes.size() == 0){

                        Log.d("TAG4", "scan_barcode.OnSucessListener IS_CHOOSEN : " + is_choosen);

                        if (is_choosen){
                            Intent intent = new Intent(getApplicationContext(), Result_Activity.class);
                            String no_barcode = "\n"+ "No Barcode Detected in your given Image,try again !" + "\n"+"\n" +
                                    "Reasons : " + "\n" + "1] No Actual/Real Barcode in the Image" + "\n" +
                                    "2] The Image does'nt show the barcode clearly";
                            intent.putExtra("data",no_barcode);
                            startActivity(intent);
                        }

                    }

                   if (barcodes.size() > 0){

                       Intent intent = new Intent(getApplicationContext(), Result_Activity.class);
                       int barcodes_detected = barcodes.size();
                       String data = "Barcodes Detected : " + barcodes_detected + "\n" + "\n";


                       int i = 0;
                       for (Barcode barcode : barcodes) {

                           String rawValue = barcode.getRawValue();
                           int valueType = barcode.getValueType();
                           String barcode_type = barcode_types.get(valueType);

                           if (valueType == Barcode.TYPE_WIFI) {

                               i+=1;
                               vibrate_device();
                               Barcode.WiFi wifiInfo = barcode.getWifi();
                               String ssid = wifiInfo.getSsid();
                               String password = wifiInfo.getPassword();
                               int type = wifiInfo.getEncryptionType();

                               data = data + "Barcode no : "+ i + "\n"+"Code Type : " + barcode_type + "\n" + "RawValue : " + rawValue + "\n" + "Encryption : " +
                                       type + "\n" + "Ssid : " + ssid + "\n" + "Password : " + password + "\n" + "\n";


                           } else if (valueType == Barcode.TYPE_URL) {

                               i+=1;
                               vibrate_device();
                               String url = Objects.requireNonNull(barcode.getUrl()).getUrl();
                               data = data + "Barcode no : "+ i + "\n"+"Code Type : " + barcode_type + "\n" + "RawValue : " + rawValue + "\n"
                                       + "Url : " + url + "\n" + "\n";


                           } else if (valueType == Barcode.TYPE_EMAIL) {

                               i+=1;
                               vibrate_device();
                               data = data + "Barcode no : "+ i + "\n"+"Code Type : " + barcode_type + "\n" + "RawValue : " + rawValue + "\n"
                                       + "\n";


                           } else if (valueType == Barcode.TYPE_GEO) {

                               i+=1;
                               vibrate_device();
                               Barcode.GeoPoint geoInfo = barcode.getGeoPoint();
                               double latitude = geoInfo.getLat();
                               double longitude = geoInfo.getLng();
                               data = data + "Barcode no : "+ i + "\n"+"Code Type : " + barcode_type + "\n" + "Latitude : " + latitude + "\n" + "Longitude : " +
                                       longitude + "\n" + "RawValue : " + rawValue + "\n" + "\n";


                           } else if (valueType == Barcode.TYPE_CONTACT_INFO) {

                               i+=1;
                               vibrate_device();

                               Barcode.ContactInfo contactInfo = barcode.getContactInfo();
                               String name = contactInfo.getName().toString();
                               String address = contactInfo.getAddresses().toString();
                               String phone = contactInfo.getPhones().toString();
                               String mail = contactInfo.getEmails().toString();
                               data = data + "Barcode no : "+ i + "\n"+"Code Type : " + barcode_type + "\n" + "RawValue : " + rawValue + "\n" + "Name : " + name + "\n" + "Address : " + address + "\n" +
                                       "Phone : " + phone + "\n" + "mail : " + mail + "\n" + "\n";


                           } else {
                               vibrate_device();
                               i+=1;
                               data = data + "Barcode no : "+ i + "\n"+"Code Type : " + barcode_type + "\n" + "RawValue : " + rawValue
                                       + "\n" + "\n";

                           }

                       }

                       // To avoid sending multiple intents
                       // we want to send intents only when user is on scan screen
                       if (!getLifecycle().getCurrentState().toString().equals("RESUMED")){
                           return;
                       }

                       intent.putExtra("data", data);
                       startActivity(intent);
                   }

                });

    }


    //=============================================================================


    private void ask_permissions() {

        // asks camera permission to the user.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, CAMERA_REQUEST_CODE);
        }

        // if permission already granted then load camera preview
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            load_camera_preview();
        }

    }

    // Called when a request permission is denied or accepted.
    // Load camera preview in both cases
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            load_camera_preview();
        } else {
            load_camera_preview();
        }
    }


    //=============================================================================

    public class Box extends View {
        private Paint paint = new Paint();

        Box(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Override the onDraw() Method
            super.onDraw(canvas);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(2);

            // center coordinates of canvas
            int x = getWidth() / 2;
            int y = getHeight() / 2;

            // Top left and Bottom right coordinates of rectangle
            int x_topLeft = x - (getWidth() / 4);
            int y_topLeft = y - (getHeight() / 4);
            int x_bottomRight = x + (getWidth() / 4);
            int y_bottomRight = y + (getHeight() / 10);

            LEFT = x_topLeft;
            RIGHT = x_bottomRight;
            TOP = y_topLeft;
            BOTTOM = y_bottomRight;

            //draw guide box
            canvas.drawRect(LEFT, TOP, RIGHT, BOTTOM, paint);

        }
    }


    private void draw_preview_rectangle() {

        Box box = new Box(this);
        addContentView(box, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

    }

    //===========================================================================

    private void vibrate_device() {

        final VibrationEffect vibrationEffect1;
        // get the VIBRATOR_SERVICE system service
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // this is the only type of the vibration which requires system version Oreo (API 26)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            // this effect creates the vibration
            vibrationEffect1 = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE);

            // it is safe to cancel other vibrations currently taking place
            vibrator.cancel();
            vibrator.vibrate(vibrationEffect1);
        }
    }

    // Converts Image to Bitmap
    private Bitmap ImageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void flash_light_switch(){

        CameraInfo cameraInfo = mCamera.getCameraInfo();
        boolean isFlashAvailable = cameraInfo.hasFlashUnit();

        if (!isFlashAvailable) {
            showNoFlashError();
        }

        if (IS_FLASH_ON){
            IS_FLASH_ON = false;
            flash_light_imageview.setImageResource(R.drawable.flashlight_off_icon);
        }else {
            IS_FLASH_ON = true;
            flash_light_imageview.setImageResource(R.drawable.flashlight_on_icon);
        }
        mCamera.getCameraControl().enableTorch(IS_FLASH_ON);

    }

    public void showNoFlashError() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .create();
        alert.setTitle("Oops!");
        alert.setMessage("Flash not available in this device...");
        alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alert.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        flash_light_imageview.setImageResource(R.drawable.flashlight_off_icon);
    }


}















