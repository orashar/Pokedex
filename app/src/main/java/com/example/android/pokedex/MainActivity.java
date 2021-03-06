package com.example.android.pokedex;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


//import TensorFlow libraries
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

// Import image utilities
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class MainActivity extends AppCompatActivity {


    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "file:///android_asset/frozen_model_CIFAR.pb";
    /*
    Please note that the name of input and output nodes should match that of names we declared in
    our TensorFlow graph
     */

    private static final String INPUT_NODE = "ipnode"; // our input node
    private static final String OUTPUT_NODE = "opnode"; // our output node

    private static final int[] INPUT_SIZE = {1,32,32,3};

    static {
        System.loadLibrary("tensorflow_inference");
    }

    // helper function to find the indices of the element in an array with maximum value
    public static int argmax (float [] elems)
    {
        int bestIdx = -1;
        float max = -1000;
        for (int i = 0; i < elems.length; i++) {
            float elem = elems[i];
            if (elem > max) {
                max = elem;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    InputStream fil;
    AssetManager assetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inferenceInterface = new TensorFlowInferenceInterface();
        inferenceInterface.initializeTensorFlow(getAssets(), MODEL_FILE);
        System.out.println("model loaded successfully");
        String imageUri = "drawable://" + R.drawable.models;

        assetManager = getAssets();

        Button selectImgbtn = findViewById(R.id.select_image_btn);
        try {

            fil = assetManager.open("bird.jpg");

        } catch (IOException e) {
            Log.e("FileNotFoundInAssets", "No file named bird.jpg");
        }
        predict(fil);
        selectImgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);

            }
        });

    }

    private void predict(InputStream file){
        try {



            final int inputSize=32;

            final int destWidth = 32;
            final int destHeight = 32;


            /*
            Mean and standard deviation of the Dataset
            Initialise them with corresponding values

            int imageMean;
            float imageStd;
             */
            // Load the image


            Bitmap bitmap = BitmapFactory.decodeStream(file);


            Bitmap bitmap_scaled = Bitmap.createScaledBitmap(bitmap, destWidth, destHeight, false);

            // Set the bitmap to the image view (UI) - optional
            ImageView image= (ImageView) findViewById(R.id.car);
            image.setImageBitmap(bitmap);


            // Load class names of CIFAR 10 dataset into a string array
            String[] classes = {"airplane","automobile","bird", "cat", "deer", "dog", "frog "," horse", "ship", "truck"};



            int[] intValues = new int[inputSize * inputSize]; // array to copy values from Bitmap image
            float[] floatValues = new float[inputSize * inputSize * 3]; // float array to store image data

            // note: Both intValues and floatValues are flattened arrays

            //get pixel values from bitmap image and store it in intValues
            bitmap_scaled.getPixels(intValues, 0, bitmap_scaled.getWidth(), 0, 0, bitmap_scaled.getWidth(), bitmap_scaled.getHeight());
            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                /*
                preprocess image if required
                floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
                floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
                floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
                */

                // convert from 0-255 range to floating point value
                floatValues[i * 3 + 0] = ((val >> 16) & 0xFF);
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF);
                floatValues[i * 3 + 2] = (val & 0xFF);
            }



            //  the input size node that we declared earlier will be a parameter to reshape the tensor
            // fill the input node with floatValues array
            inferenceInterface.fillNodeFloat(INPUT_NODE, INPUT_SIZE, floatValues);
            // make the inference
            inferenceInterface.runInference(new String[] {OUTPUT_NODE});
            // create an array filled zeros with dimension of number of output classes. In our case its 10
            float [] result = new float[10];
            Arrays.fill(result,0.0f);
            // copy the values from output node to the 'result' array
            inferenceInterface.readNodeFloat(OUTPUT_NODE, result);
            // find the class with highest probability
            int class_id=argmax(result);
            TextView textView=(TextView) findViewById(R.id.result);
            // Setting the class name in the UI
            textView.setText(classes[class_id]);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPathFromUri(Uri contentUri){
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){
            res = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        }
        cursor.close();
        return res;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try{
            if(resultCode == RESULT_OK){
                if(requestCode == 1){
                    Uri selectedUri = data.getData();
                    fil = getContentResolver().openInputStream(selectedUri);
                    predict(fil);
                    /*final String path = getPathFromUri(selectedUri);

                    if(path != null){
                        File f = new File(path);
                        InputStream fi =
                        selectedUri = Uri.fromFile(f);
                    }*/
                }
            }
        } catch (Exception e){
            Log.e("ImageSelectionError", e.getMessage());
        }
    }
}