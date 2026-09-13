package com.gai.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** 结构导入中转页：调用系统文件选择器选择 .mcstructure 文件 */
public class ImportActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream", "application/zip", "*/*"});
        try {
            startActivityForResult(Intent.createChooser(i, "选择 .mcstructure 结构文件"), 1);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            new Thread(new Runnable() {
                public void run() {
                    final String msg = StructureImporter.importStructure(ImportActivity.this, uri);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }).start();
        }
        finish();
    }
}
