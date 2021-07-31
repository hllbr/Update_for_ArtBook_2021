package com.hllbr.update_for_artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.hllbr.update_for_artbook.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding artBinding;
    ActivityResultLauncher<Intent> activityResultLauncher; //Galeriye gitmek için kullanıcam
    ActivityResultLauncher<String> permissionLauncher ;//izinlerde stringler ile işimizi yapıyoruz.
    Bitmap selectedImage;
    SQLiteDatabase database ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_art);
        artBinding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = artBinding.getRoot();
        setContentView(view);
        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        registerLauncher();
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
       // database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
        if (info.equals("new")){
            //new Art
            artBinding.nameText.setText("");
            artBinding.artistText.setText("");
            artBinding.yearText.setText("");
            artBinding.button.setVisibility(View.VISIBLE);
            artBinding.imageView.setImageResource(R.drawable.addimage);
        }else {
            //old Art
            int artıD = intent.getIntExtra("artId",0);
            artBinding.button.setVisibility(View.INVISIBLE);
            try {
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artıD)});
                int artNameIx = cursor.getColumnIndex("artname");
                int paintername = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx  = cursor.getColumnIndex("image");
                while (cursor.moveToNext()){
                    artBinding.nameText.setText(cursor.getString(artNameIx));
                    artBinding.artistText.setText(cursor.getString(paintername));
                    artBinding.yearText.setText(cursor.getString(yearIx));
                    byte [] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    artBinding.imageView.setImageBitmap(bitmap);
                }
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(ArtActivity.this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
            }

        }

    }
    public void save(View view){
    /*
    SQLite içerisinde boyut sınırlandırması var Kullanıcının kaç mb bir dosyayı kaydetmek isteyeceğini bilmiyoruz.Veri tabanı içerisine görsel kaydetmek verimli bir yöntem değil Sebebi ise görsel kaydettikçe veri tabanını şişiriyoruz.
    1 MB üzerine çıkyığımızda bize hata verebilir.Günümüz telefonlasrı göz önünüe alındığında Kamera ile çekilen bir görsel 2 - 3 MB alan kaplıyor.
    Bizim bunu küçültmemiz gerekiyor.
    try-catch içerisinde yaptığımızda app çökmez fakat verimiz kayıt olmaz
     */
        String name = artBinding.nameText.getText().toString();
        String artistName = artBinding.artistText.getText().toString();
        String year = artBinding.yearText.getText().toString();


        Bitmap smallImage = makeSmallerImage(selectedImage,300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try{
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();
/*
            //database.execSQL("INSERT INTO (artname,paintername,year,image) VALUES(?,?,?,?)");
            String sqlString = "INSERT INTO arts(artname,paintername,year,image) VALUES(?,?,?,?)";
           // SQLiteStatement biniding işlemlerimi kolay yapmamızı sağlayan bir yapı
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            //kolaylığını alt satırda görebiliyoruz.
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindDouble(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();
*/
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(ArtActivity.this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }
        //finish();//concept1
        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//CONCEPT_2
        startActivity(intent);

    }

    public Bitmap makeSmallerImage(Bitmap image,int maximumSize){
        int width =image.getWidth();
        int height = image.getHeight();
        Double bitmapRatio = (double) width / (double) height;
        if (bitmapRatio > 1){
            //landscape image
            width = maximumSize;
            height = (int) (width /maximumSize) ;
        }else{
            //portrait image
            height = maximumSize;
            width = (int)(height * maximumSize);

        }
        return image.createScaledBitmap(image,width,height,true);
    }

    public void selectImage(View view){
        //We need User Permission Control
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //request permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
               // Snackbar ve Toast yapılma mantıkları aynı sadece aradaki fark olarak parametrelerin değiştiğini söyleyebiliriz
                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }else {
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            /*
            Android 2021 ortası itibari ile eğer kullanıcı istenilen izni vermeyi redderse ona mantık gösterebiliyoruz.
            Normalde Galeri için düşünürsek izin veriyor musun vermiyor musun şeklinde soruluyordu gelen değişiklikler ile birlikte
            Galerindeki seçeceğin resmi sana göstermem lazım gibi bir bilgilendirme yapılmasını mantıklı buldu temelde kullanıcıyı daha fazla bilgilendirerek bu işlemin gerçekleştirilmesini sağlamak istiyoruz.
            Kullanıcının neye izin verdiğinin farkında olması üzerinde geliştirilmiş bir yapıdır.
            Bu yapıda otomatik olarak geliyor bizim ekstra bir çaba harcamamızı gerektirecek bir durum yok ...
            Şuan eski yöntemden bağımsız olarak request permission yapmadan kullanıcıya açıklama göstermek zorundamıyız onu kontrol ediyoruz.
            shouldShowRequestPermissionRationale() -> izin isteme mantığını kullanıcıya göstereyim mi kullanıcıya soruyoruz.
            Şuan bu yapı opsiyonel fakat ilerleyen zamanlarda zorunlu olması planlanmaktadır.
             */
        }else {
            //gallery operation
            Intent intentToGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);

            /*
            startActivityforResult eski bir yöntem bunun bir alternatifi üretildi.
            startActivityforResult = bir sonuç için activiteyi başlat anlamına geliyor.
            Android 2021 itibariyle bunu yapıdan vazgeçti
            ActivityResutLauncher = Activite sonucu başlatıcı oalrak ifade edebiliriz.
            Biz yeni bir activite açıp yada galeriye gidip ordan bir veri alıp onu ele almak mı istiyoruz
            Biz bir izin isteyip bu izin verildiğinde ne olacağını yazmak istiyorsak hepsi için bu yapı işimizi görüyor.
            işlemin sonucunua göre işlem yapma esnekliğimiz oluyor bu şekilde.
            Bu yapıları onCreate altında uygulama daha başlamadan register etmek kayıt etmek gibi bir zorunluluğumuz bulunuyor.

             */
                    //Intent.ACTION_PICK == TUTUP ALMAK GİBİ DÜŞÜNÜLEBİLİR.
        }
    }

    public  void registerLauncher(){
        //All regist operation in this area =
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK){
                    //Kullanıcı birşeyler seçtiğinde
                    Intent intentFromResult = result.getData();//veriyi alma işlemi//bu yapı bize intent verdi
                    if (intentFromResult != null){
                        Uri imageData =  intentFromResult.getData();//burada getData bize Uri verdi yani kullanıcının seçtiği görselin nerede kayıtlı olduğnu veren bir yapımız olmuş oldu
                       // artBinding.imageView.setImageURI(imageData);//Bize kullanıcının resminin nerede kayıtlı olduğu değil verisi lazım çünkü biz veriyi alıp veri tabanına kaydetmek zorundayız.
                        //Bu yapı sadece resimi imageView üzerinde göstermeye yarıyor . Bizim bu resmi aynı zamanda bitmap'e dönüştürmemiz gerekiyor.
                        try {//Dene
                            if (Build.VERSION.SDK_INT >= 28){
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                artBinding.imageView.setImageBitmap(selectedImage);
                            }else{
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                                artBinding.imageView.setImageBitmap(selectedImage);
                            }

                        }catch(Exception e){//Yakala = Uygulamayı çökertebilecek bir sıkıntı olursa bu noktada yakala
                            e.printStackTrace();
                            Toast.makeText(ArtActivity.this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
           if (result){
               //permission granted
               //Gallery Operation
               Intent intentToGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
           }else{
               //permission Denied
               Toast.makeText(ArtActivity.this,"permission needed!",Toast.LENGTH_LONG).show();
           }
            }
        });
    }
}