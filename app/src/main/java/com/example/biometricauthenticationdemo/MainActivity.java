package com.example.biometricauthenticationdemo;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private KeyStore keyStore;  //helps to access the stored equation

    private static final String KEY_NAME="mandar";
    private Cipher cipher;
    private TextView textView;
    public KeyguardManager keyguardManager;         //stores patterns,passwords,fingerprints,biometric data and acanaccesss it
    public FingerprintManager fingerprintManager;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        keyguardManager=(KeyguardManager) getSystemService(KEYGUARD_SERVICE);       //Calling keyguard service
        fingerprintManager=(FingerprintManager) getSystemService(FINGERPRINT_SERVICE);  //Calling Fingerprint service

        textView=(TextView) findViewById(R.id.errortext);
        checkFingerprintScanner();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkFingerprintScanner() {
        //check whether the device has a fingerprint sensor.
        if(!fingerprintManager.isHardwareDetected())
        {
            /**
             * an error message will be dsiplayed if the device does not contain the fingerprint
             * however if you plan to implement a default authentication method
             */
            textView.setText("Your Device does not have a Fingrtprint Sensor");
        }
        else
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT)!=
                    PackageManager.PERMISSION_GRANTED)
            {
                textView.setText("Fingerprint authentication permission is not enabled");
            }
            else
            {
                if (!fingerprintManager.hasEnrolledFingerprints())
                {
                    textView.setText("Register at least one fingerprint in settings");
                }
                else
                {
                    //checks whether lock screen security is enabled or not
                    if(!keyguardManager.isKeyguardSecure())
                    {
                        textView.setText("Lock screen security not enabled in settings");
                    }
                    else
                    {
                        generateKey();
                        if (cipherInit())
                        {
                            FingerprintManager.CryptoObject cryptoObject=new FingerprintManager.CryptoObject(cipher);
                            FingerprintAuthenticationHandler helper=new FingerprintAuthenticationHandler(this);
                            helper.startAuth(fingerprintManager,cryptoObject);
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean cipherInit() {
        try{
            cipher=Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES+"/"+KeyProperties.BLOCK_MODE_CBC+"/"+KeyProperties.ENCRYPTION_PADDING_PKCS7);
        }catch (NoSuchAlgorithmException | NoSuchPaddingException e){
            throw new RuntimeException("Failed to get Cipher",e);
        }

        try{
            keyStore.load(null);
            SecretKey key=(SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE,key);
            return true;
        }catch (KeyPermanentlyInvalidatedException e){
            return false;
        }catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e){
            throw new RuntimeException("Failed to init Cipher",e);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateKey() {
        try{
            keyStore=KeyStore.getInstance("AndroidKeyStore");
        }catch (Exception e){
            e.printStackTrace();
        }

        KeyGenerator keyGenerator;
        try{
            keyGenerator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");   //AES:-Android Encryption Standards(functions on public and privtae keys)
        }catch (NoSuchAlgorithmException | NoSuchProviderException e){
            throw new RuntimeException("Failed to get KeyGenerator instance",e);
        }

        try{
            keyStore.load(null);
            //Parameters for key generation(Key name,Encryption,Decryption)
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)        //Cipher Block Chaining is a methodology or a technique
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_PKCS7)     //adds dummy or redundant data between original data so it cant be decrypted
            .build());
            keyGenerator.generateKey();
        } catch (IOException | NoSuchAlgorithmException | InvalidAlgorithmParameterException |CertificateException  e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
