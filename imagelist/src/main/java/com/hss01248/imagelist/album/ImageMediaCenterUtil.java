package com.hss01248.imagelist.album;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.hss01248.image.ImageLoader;
import com.hss01248.image.MyUtil;
import com.hss01248.image.interfaces.FileGetter;
import com.hss01248.imagelist.NormalCallback;
import com.hss01248.imagelist.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * time:2019/11/30
 * author:hss
 * desription:
 */
public class ImageMediaCenterUtil {

    public static void getAlbums(final Context context, final NormalCallback<List<Album>> callback) {

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String[] projection = new String[]{
                        MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        MediaStore.Images.Media.DATA};

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);


                Cursor cursor = context.getApplicationContext().getContentResolver()
                        .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                                null, null, MediaStore.Images.Media.DATE_ADDED);
                if (cursor == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFail(new Exception("no image found on this devices"));
                        }
                    });
                    return;
                }

                final ArrayList<Album> albums = new ArrayList<>(cursor.getCount());
                HashSet<Long> albumSet = new HashSet<>();
                File file;
                if (cursor.moveToLast()) {
                    do {
                        if (Thread.interrupted()) {
                            return;
                        }

                        long albumId = cursor.getLong(cursor.getColumnIndex(projection[0]));
                        String album = cursor.getString(cursor.getColumnIndex(projection[1]));
                        String image = cursor.getString(cursor.getColumnIndex(projection[2]));
                        //int size = cursor.getInt(cursor.getColumnIndex(COLUMN_COUNT));

                        if (!albumSet.contains(albumId)) {
                        /*
                        It may happen that some image file paths are still present in cache,
                        though image file does not exist. These last as long as media
                        scanner is not run again. To avoid get such image file paths, check
                        if image file exists.
                         */
                            file = new File(image);
                            Log.d("ImageMediaCenterUtil",file.getAbsolutePath());
                            if (file.exists()) {
                                Album album1 = new Album(album, image,albumId);
                                albums.add(album1);
                                albumSet.add(albumId);

                                //File dir = file.getParentFile();
                               // File[] files = dir.listFiles();
                                int count = 0;
                                long fileSize = 0;
                                /*for (File file1 : files) {
                                    if(isImage(file1)){
                                    count++;
                                    fileSize = fileSize + file1.length();
                                    }
                                }*/
                                album1.count = count;
                                album1.fileSize = fileSize;
                            }
                        }

                    } while (cursor.moveToPrevious());
                }
                cursor.close();


                //按文件大小排序:
                /*Collections.sort(albums, new Comparator<Album>() {
                    @Override
                    public int compare(Album o1, Album o2) {
                        return (int) (o2.fileSize - o1.fileSize);
                    }
                });*/

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(albums,null);
                    }
                });

            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public static void listImagesByAlbumName(final Context context, final long albumId, final NormalCallback<List<Image>> callback){
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                 String[] projection = new String[]{MediaStore.Images.Media._ID,
                         MediaStore.Images.Media.DISPLAY_NAME,
                         MediaStore.Images.Media.DATA,
                         MediaStore.Images.Media.SIZE,
                         MediaStore.Images.Media.DATE_ADDED,
                         MediaStore.Images.Media.DATE_MODIFIED,
                         MediaStore.Images.Media.WIDTH,
                         MediaStore.Images.Media.HEIGHT,
                         MediaStore.Images.Media.MIME_TYPE};

                android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                        MediaStore.Images.Media.BUCKET_ID + " =?", new String[]{albumId+""}, MediaStore.Images.Media.DATE_ADDED);
                if (cursor == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFail(new Exception("no image found on this devices"));
                        }
                    });
                    return;
                }

            /*
            In case this runnable is executed to onChange calling loadImages,
            using countSelected variable can result in a race condition. To avoid that,
            tempCountSelected keeps track of number of selected images. On handling
            FETCH_COMPLETED message, countSelected is assigned value of tempCountSelected.
             */

                 final ArrayList<Image> images = new ArrayList<>(cursor.getCount());
                if (cursor.moveToLast()) {
                    do {
                        if (Thread.interrupted()) {
                            return;
                        }

                        //long id = cursor.getLong(cursor.getColumnIndex(projection[0]));
                       // String name = cursor.getString(cursor.getColumnIndex(projection[1]));
                        String path = cursor.getString(cursor.getColumnIndex(projection[2]));


                        File file = new File(path);
                        Log.i("path", path);
                        if (file.exists()) {
                            images.add(new Image(
                                    cursor.getLong(cursor.getColumnIndex(projection[0])),//_ID
                                    cursor.getString(cursor.getColumnIndex(projection[1])),//DISPLAY_NAME
                                    path,//DATA
                                    cursor.getLong(cursor.getColumnIndex(projection[3])),//SIZE
                                    cursor.getLong(cursor.getColumnIndex(projection[4])),//DATE_ADDED
                                    cursor.getLong(cursor.getColumnIndex(projection[5])),//DATE_MODIFIED
                                    cursor.getLong(cursor.getColumnIndex(projection[6])),//WIDTH
                                    cursor.getLong(cursor.getColumnIndex(projection[6])),//HEIGHT
                                    cursor.getString(cursor.getColumnIndex(projection[7]))//MIME_TYPE



                            ));
                        }

                    } while (cursor.moveToPrevious());
                }
                cursor.close();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(images,null);
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * 不压缩png,因为会变黑,效果不好
     * @param pathname
     * @return
     */
    public static boolean isImage(File pathname) {

        if(pathname.isDirectory()){
            return false;
        }
        String name = pathname.getName();
        int idx = name.lastIndexOf(".");
        if(idx <0 || idx >= name.length()-1){
            return false;
        }
        String suffix = name.substring(idx+1);
        boolean isJpg = suffix.equalsIgnoreCase("jpg")
                || suffix.equalsIgnoreCase("jpeg")
                || suffix.equalsIgnoreCase("png")
                || suffix.equalsIgnoreCase("gif")
                ||  suffix.equalsIgnoreCase("webp")
                ||  suffix.equalsIgnoreCase("raw");
        return  isJpg;
    }

    public static void showBigImag(Context context,List<String> urlsOrPaths,int position){

        View view = buildBigIamgeView(context,urlsOrPaths,position);


        showViewAsDialog(view);
    }

    public static void showViewAsDialog(View view) {
        Dialog dialog = new Dialog(view.getContext());
        dialog.setContentView(view,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));//背景颜色一定要有，看自己需求
        dialog.getWindow().setLayout(view.getResources().getDisplayMetrics().widthPixels,view.getResources().getDisplayMetrics().heightPixels);//宽高最大
        dialog.show();
    }


    private static View buildBigIamgeView(final Context context, final List<String> urls, int position) {
        View view = View.inflate(context,R.layout.activity_img_big,null);
        final ViewPager  viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        Button fab = view.findViewById(R.id.fbtn);
        final TextView textView = view.findViewById(R.id.tv_indicator);
        ImageLoader.loadBigImages(viewPager,urls);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                String text = (position+1)+" / "+ urls.size()+"\n";
                textView.setText(text);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileInfo(viewPager,urls,context);
            }
        });
        viewPager.setCurrentItem(position);
        return view;
    }

    private static void showFileInfo(ViewPager viewPager, List<String> urls, final Context context) {
        int position = viewPager.getCurrentItem();
        String url = urls.get(position);

        ImageLoader.getActualLoader().getFileFromDiskCache(url, new FileGetter() {
            @Override
            public void onSuccess(File file, int width, int height) {
                String text = file.getAbsolutePath();
                text += "\n" + MyUtil.formatFileSize(file.length());
                text += "\nw"+ width+"x"+height +"\n";
                text += MyUtil.printExif(file.getAbsolutePath());

                View view = View.inflate(context,R.layout.html,null);
                TextView textView = view.findViewById(R.id.tv_html);
                textView.setText(text);
                new AlertDialog.Builder(context)
                        .setView(view)
                        .create().show();
            }

            @Override
            public void onFail(Throwable e) {
                e.printStackTrace();

            }
        });





    }


}
