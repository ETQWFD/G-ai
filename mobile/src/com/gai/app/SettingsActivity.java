package com.gai.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** 设置界面：自定义 AI 配置（地址 / Key / 模型，支持自动搜索）与连接端口 */
public class SettingsActivity extends Activity {

    private EditText etUrl, etKey, etModel, etPort;
    private TextView saveResult;
    private boolean searching;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        etUrl = (EditText) findViewById(R.id.etUrl);
        etKey = (EditText) findViewById(R.id.etKey);
        etModel = (EditText) findViewById(R.id.etModel);
        etPort = (EditText) findViewById(R.id.etPort);
        saveResult = (TextView) findViewById(R.id.saveResult);
        final android.widget.CheckBox cbAware = (android.widget.CheckBox) findViewById(R.id.cbAware);

        etUrl.setText(Prefs.url(this));
        etKey.setText(Prefs.key(this));
        etModel.setText(Prefs.model(this));
        etPort.setText(String.valueOf(Prefs.port(this)));
        cbAware.setChecked(Prefs.aiAware(this));

        findViewById(R.id.btnAuto).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { autoSearch(); }
        });
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(); }
        });
        findViewById(R.id.btnUpdate).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Updater.checkAndPrompt(SettingsActivity.this, null); }
        });
    }

    private void autoSearch() {
        final String url = etUrl.getText().toString().trim();
        final String key = etKey.getText().toString().trim();
        if (url.isEmpty() || key.isEmpty()) {
            toast("请先填写接口完整地址与 API Key");
            return;
        }
        if (searching) return;
        searching = true;
        final Button btn = (Button) findViewById(R.id.btnAuto);
        btn.setText("搜索中…");
        new Thread(new Runnable() {
            public void run() {
                try {
                    final List<String> models = AiClient.listModels(url, key);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            searching = false;
                            btn.setText("自动搜索：从该 Key 中查找可用模型");
                            if (models.isEmpty()) {
                                saveResult.setText("未找到可用模型，请手动输入模型名称");
                                saveResult.setTextColor(0xFFDC2626);
                            } else {
                                showModelPicker(models);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            searching = false;
                            btn.setText("自动搜索：从该 Key 中查找可用模型");
                            saveResult.setText("搜索失败：" + e.getMessage());
                            saveResult.setTextColor(0xFFDC2626);
                        }
                    });
                }
            }
        }).start();
    }

    private void showModelPicker(final List<String> models) {
        final String[] arr = models.toArray(new String[0]);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("找到 " + models.size() + " 个可用模型，请选择");
        b.setItems(arr, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                etModel.setText(arr[which]);
                saveResult.setText("已选择模型：" + arr[which]);
                saveResult.setTextColor(0xFF065F46);
            }
        });
        b.setNegativeButton("取消（手动输入）", null);
        b.show();
    }

    private void save() {
        String url = etUrl.getText().toString().trim();
        String key = etKey.getText().toString().trim();
        String model = etModel.getText().toString().trim();
        String portS = etPort.getText().toString().trim();
        if (url.isEmpty()) { toast("请填写接口完整地址"); return; }
        if (key.isEmpty()) { toast("请填写 API Key"); return; }
        if (model.isEmpty()) { toast("请填写模型名称，或先点击「自动搜索」"); return; }
        int port = 8080;
        try {
            port = Integer.parseInt(portS);
            if (port < 1024 || port > 65535) port = 8080;
        } catch (Exception e) {
            port = 8080;
        }
        Prefs.setUrl(this, url);
        Prefs.setKey(this, key);
        Prefs.setModel(this, model);
        Prefs.setPort(this, port);
        try {
            android.widget.CheckBox cbAware = (android.widget.CheckBox) findViewById(R.id.cbAware);
            Prefs.setAiAware(this, cbAware.isChecked());
        } catch (Exception e) { /* ignore */ }
        saveResult.setText("保存成功！返回主界面点击「启动悬浮窗」即可使用");
        saveResult.setTextColor(0xFF6EE7B7);
        toast("配置已保存");
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
