package cn.wangdazhuang.zdict;

import gnu.inet.encoding.IDNA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import cn.wangdazhuang.zdict.util.BufferedRandomAccessFile;
import cn.wangdazhuang.zdict.util.ByteConverter;
import cn.wangdazhuang.zdict.util.DictZipHeader;
import cn.wangdazhuang.zdict.util.DictZipInputStream;
import cn.wangdazhuang.zdict.util.FileAccessor;
import cn.wangdazhuang.zdict.util.GB2Alpha;

/**
 * 大壮词典 Activity 主类
 * @author wangxinxuan
 */
public class ZhuangDictActivity extends Activity implements TextToSpeech.OnInitListener {
	/**
	 * 记录更新信息SharedPreferences文件名
	 */
	public static final String UPDATE_SHAREDPREFERENCES_FILE = "update_ver.pre";
	/**
	 * 获取存储卡根目录
	 */
	private static final File SD_ROOT = android.os.Environment.getExternalStorageDirectory();
	/**
	 * 位于存储卡的词典目录
	 */
	private static final String DICT_PATH = SD_ROOT.getAbsolutePath() + File.separator + "zdict";
	/**
	 * 内部链接正则表达式
	 */
	private static final String LINK_PATTERN = "bword://";

	/**
	 * 版本信息PackageManager
	 */
	private PackageManager packageManager;
	/**
	 * 软件版本名称，暂不使用
	 */
	// private String versionName;
	/**
	 * 默认数据库版本（同软件版本）
	 */
	protected static int versionCode = 1;

	/**
	 * 数据库存储类
	 */
	private DatabaseHelper databaseHelper;
	/**
	 * 汇总INSERT_NUM条记录，然后进行一次Insert
	 */
	private static final int INSERT_NUM = 2000;

	/**
	 * 用户选项config文件
	 */
	private static final String PREFERENCES_FILE = "zhuang_preferences";
	/**
	 * 用户选项config文件中保存的最后使用的词典信息
	 */
	private static final String PREFERENCES_DICT_NAME = "dict_name";
	/**
	 * 用户选项config文件中保存的字体缩放信息
	 */
	private static final String PREFERENCES_SCALE_IN_PERCENT = "scale_in_percent";
	/**
	 * 默认字体缩放百分比
	 */
	private static final int DEFAULT_SCALE_IN_PERCENT = 100;
	/**
	 * 保存设置后的字体缩放百分比
	 */
	private int scaleInPercent;
	/**
	 * 更新信息SharedPreferences
	 */
	private SharedPreferences sharedPreferences;
	/**
	 * 默认自动完成查询数量，0为不限制
	 */
	private static final int DEFAULT_AUTOCOMPLETE_LIMIT = 0;

	/**
	 * 词典信息
	 */
	private String dictInfo;
	/**
	 * 词典文件
	 */
	private File dictFile;
	/**
	 * 词典文件名
	 */
	private String dictName;
	/**
	 * 词典文件名列表
	 */
	private String[] dictFilenames;
	/**
	 * 词典数据库表名
	 */
	private String tableName;
	/**
	 * 词典索引文件路径
	 */
	private String idxFileName;
	/**
	 * 词典信息文件路径
	 */
	private String ifoFileName;
	/**
	 * 词典文件路径
	 */
	private String dictFileName;
	/**
	 * 词典数据库文件名
	 */
	private String dbFileName;

	/**
	 * true表示当前词典是为dz压缩词典
	 */
	private static boolean isDZDict = false;

	/**
	 * 词典文件根目录
	 */
	private File dictPathFile;
	/**
	 * 词典数据库文件
	 */
	protected static File dbFile;

	/**
	 * 查询单词
	 */
	private String word;
	/**
	 * 查询单词结果
	 */
	private String wordDefinition = "";
	
	// 词典相关属性
	/**
	 * 当前索引字符
	 */
	private static char currentChar = 0;
	/**
	 * 存储InsertHelper的HashMap
	 */
	private static HashMap<Character, InsertHelper> insertHelperMap;
	/**
	 * InsertHelper实例
	 */
	private InsertHelper insertHelper = null;
	/**
	 * 词典名称
	 */
	private String bookName = null;
	/**
	 * 词典共有单词数量
	 */
	private String wordCount = null;
	/**
	 * 索引文件大小
	 */
	private String idxFileSize = null;

	/**
	 * 单词索引库列定义
	 */
	private static final int wordIndex = 1;
	/**
	 * 单词索引库列偏移量
	 */
	private static final int offsetIndex = 2;
	/**
	 * 单词索引库列数据大小
	 */
	private static final int sizeIndex = 3;
	
	// 历史记录相关
	/**
	 * 最多保存HISTORY_NUM个查询历史记录数
	 */
	private static final byte HISTORY_NUM = 10;
	/**
	 * 查询单词历史记录
	 */
	private String[] historyWord = new String[HISTORY_NUM];
	/**
	 * 查询单词结果历史记录
	 */
	private String[] historyResult = new String[HISTORY_NUM];
	/**
	 * 查询单词历史记录索引
	 */
	private byte historyIndex = 0;
	/**
	 * 中文默认编码
	 */
	private static final String ENCODING = "utf-8";

	// View相关定义
	/**
	 * 索引数据导入进度条
	 */
	private ProgressDialog idxImportProgressDialog;
	/**
	 * 查询输入框（自动完成）
	 */
	private AutoCompleteTextView searchAutoCompleteTextView = null;
	/**
	 * 查询按钮
	 */
	private ImageButton searchButton = null;
	/**
	 * 查询结果
	 */
	private WebView searchResultWebView = null;
	/**
	 * WebView设置对象WebSettings
	 */
	private WebSettings webSettings;
	/**
	 * TextToSpeech按钮
	 */
	private ImageButton ttsButton = null;
	/**
	 * TextToSpeech对象
	 */
	private TextToSpeech textToSpeech;
	/**
	 * 检查TextToSpeech状态
	 */
	private static final int REQ_TTS_STATUS_CHECK = 0;
	/**
	 * idx缓冲大小
	 */
	public static final int IDX_BUFFER_LENGTH = 256 + 1 + 4 + 4;

	/**
	 * 查询进度条
	 */
	private ProgressDialog searchProgressDialog;
	
	/**
	 * InputMethodManager
	 */
	private InputMethodManager imm; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// 初始化数据库
		databaseHelper = new DatabaseHelper(this);

		// 判断存储卡是否存在
		if (!android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			showErrorDialog("没有找到SD卡，请检查SD卡后再试");
		} else {

			// 获取表后缀相关类，可获取中文相关拼音
			insertHelperMap = new HashMap<Character, InsertHelper>();

			// 获取版本信息
			packageManager = this.getPackageManager();
			try {
				PackageInfo info = packageManager.getPackageInfo(this.getPackageName(), 0);
				versionCode = info.versionCode;
			} catch (NameNotFoundException e1) {
				showDialog("版本信息有误，请安装最新版本");
				e1.printStackTrace();
			}
			// 自动更新配置文件URL
			String url = getResources().getString(R.string.update_file);
			// showDialog(UpdateInfoAsyncTask.getUpdateInfo(url).toString());
			// 启动自动更新异步线程
			new UpdateInfoAsyncTask(this).execute(url, String.valueOf(versionCode));

			// 初始化控件
			searchAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.edit);
			searchAutoCompleteTextView.requestFocus();
			//展示键盘
			imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
			
			searchButton = (ImageButton) findViewById(R.id.search);
			searchResultWebView = (WebView) findViewById(R.id.result);
			webSettings = searchResultWebView.getSettings();
			webSettings.setSupportZoom(true);// 设置允许缩放
			webSettings.setBuiltInZoomControls(true);// 设置允许两点触摸缩放

			ttsButton = (ImageButton) findViewById(R.id.ts);
			
			searchProgressDialog = new ProgressDialog(this);

			// 设置WebView点击事件
			searchResultWebView.setWebViewClient(new WebViewClient() {

				@Override
				public void onScaleChanged(WebView view, float oldScale, float newScale) {
					// 点击缩放按钮进行缩放，之后将缩放结果存储，作为下一次进入程序时的默认缩放值
					scaleInPercent = Float.valueOf(newScale * 100).intValue();
					searchResultWebView.setInitialScale(scaleInPercent);
					sharedPreferences.edit().putInt(ZhuangDictActivity.PREFERENCES_SCALE_IN_PERCENT, scaleInPercent).commit();
//					super.onScaleChanged(view, oldScale, newScale);
				}

				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url.startsWith(LINK_PATTERN)) {// 如果是关联词汇查询
						word = IDNA.toUnicode(url.replace(LINK_PATTERN, ""), false, false);
						if (word.length() > 0) {
							searchAutoCompleteTextView.setText(word);
							searchAutoCompleteTextView.setSelection(word.length());// 将光标移至文字末尾
							new SearchDicAsyncTask().execute(word);
						}
					}
					if (url.startsWith("http")) {// 如果是外链则在新窗口载入
						Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						it.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
						startActivity(it);
						// 在当前窗口载入
						// view.loadUrl(url);
					}
					return true;
				}

				@Override
				public void onPageStarted(WebView view, String url,
						Bitmap favicon) {
					searchProgressDialog.setMessage("正在渲染，请稍侯");
					searchProgressDialog.show();
					super.onPageStarted(view, url, favicon);
				}

				@Override
				public void onPageFinished(WebView view, String url) {
					searchProgressDialog.dismiss();
					// TODO 需要解决Webview重新加载内容后，不自动换行的问题
					onConfigurationChanged(ZhuangDictActivity.this.getResources().getConfiguration());
					super.onPageFinished(view, url);
				}

			});
			// 语音朗读控件
			ttsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (textToSpeech != null) {
						if (searchAutoCompleteTextView.getText().toString().length() == 0) {
							makeToast("请输入单词");
						} else {
							textToSpeech.speak(searchAutoCompleteTextView.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
						}
					} else {
						makeToast("安装英文朗读引擎后才可以读单词");
					}
				}
			});

			// 查询按钮
			searchButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					searchWord();
				}
			});

			// 如果词典目录不存在则创建
			dictPathFile = new File(DICT_PATH + File.separator);
			if (!dictPathFile.exists()) {
				if (!dictPathFile.mkdir()) {
					makeToast("创建词典目录失败，请检查SD卡后再试");
				}
			}

			// 得到缓存的变量
			sharedPreferences = getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
			scaleInPercent = sharedPreferences.getInt(PREFERENCES_SCALE_IN_PERCENT, DEFAULT_SCALE_IN_PERCENT);
			dictName = sharedPreferences.getString(PREFERENCES_DICT_NAME, null);
			if (dictName == null) {
				showChooseDictDialog(false);
			} else {
				modifyDictVar(dictName);
				setTitle(loadDictInfo());

				initDbFile();
				databaseHelper.initDb();
			}
			// 设置默认缩放比例
			searchResultWebView.setInitialScale(scaleInPercent);

			// 初始化自动完成控件
			searchAutoCompleteTextView.setThreshold(1);
			searchAutoCompleteTextView.setAdapter(new WordAutoComplite(ZhuangDictActivity.this, null));
			searchAutoCompleteTextView.addTextChangedListener(new TextWatcher() {

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (s.length() > 0) {
						char key = GB2Alpha.Char2Alpha(s.charAt(0));
						if (currentChar != key) {
							currentChar = key;
							tableName = DatabaseHelper.transTableName(key);
						}
					}
				}
			});
			
			// 增加点击选择提示项搜索事件
			searchAutoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					searchWord();
				}
			});

			// 回车搜索事件
			searchAutoCompleteTextView.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
						searchWord();
						return true;
					}
					return false;
				}
			});

			// 检查TTS数据是否已经安装并且可用
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
		}
	}

	/* 记录查询的单词及结果
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("edit", word);
		outState.putString("result", wordDefinition);
	}

	/* 恢复查询的单词及结果
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// 恢复状态
		if (savedInstanceState != null) {
			searchAutoCompleteTextView.setText(savedInstanceState.getString("edit"));
			searchResultWebView.loadDataWithBaseURL(null, savedInstanceState.getString("result"), "text/html", ENCODING, null);
		}
	}

	/* 设置回退按键
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (!loadHistory(true)) {
				super.onKeyDown(keyCode, event);
			}
		}
		return false;
	}

	/* 设置菜单
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	/* 点击菜单键
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// 点击切换词典
		if (item.getTitle().equals(getResources().getString(R.string.menu_title_change_dict))) {
			showChooseDictDialog(true);
		}
		// 点击关于
		if (item.getTitle().equals(getResources().getString(R.string.menu_title_about))) {
			showDialog("欢迎使用大壮词典\n为纪念大壮2010年8月19日三周岁而作。");
		}
		// 点击退出
		if (item.getTitle().equals(getResources().getString(R.string.menu_title_exit))) {
			finish();
		}
		return false;
	}

	/* 检查Intent返回的结果，主要是用来检测TTS Engine是否可用
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_TTS_STATUS_CHECK) {
			switch (resultCode) {
			case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
				// 这个返回结果表明TTS Engine可以用
				textToSpeech = new TextToSpeech(this, this);
				// System.out.println("TTS Engine is installed!");
				break;
			case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:// 需要的语音数据已损坏
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:// 缺少需要语言的语音数据
			case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:// 缺少需要语言的发音数据
				// 这三种情况都表明数据有错,重新下载安装需要的数据
				// System.out.println("Need language stuff:" + resultCode);
				// Intent dataIntent = new Intent();
				// dataIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				// startActivity(dataIntent);
				break;
			case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:// 检查失败
			default:
				// System.out.println("Got a failure. TTS apparently not available");
				break;
			}
		} else {
			// 其他Intent返回的结果
		}
	}

	/* 实现TTS初始化接口
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	@Override
	public void onInit(int status) {
		// TTS Engine初始化完成
		if (status == TextToSpeech.SUCCESS) {
			int result = textToSpeech.setLanguage(Locale.ENGLISH);
			// 设置发音语言
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				result = textToSpeech.setLanguage(Locale.US);
				// 判断语言是否可用
				// System.out.println("Language is not available");
				// ts.setEnabled(false);
				// ts.setVisibility(View.GONE);
				// } else {
				// tts.speak("This is an example of speech synthesis.",
				// TextToSpeech.QUEUE_ADD, null);
				// ts.setEnabled(true);
			}
		}

	}

	/* 在改变屏幕方向时，不再去执行onCreate()方法，而是直接执行onConfigurationChanged()
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/* Pause时停止TTS Engine
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (textToSpeech != null) {
			textToSpeech.stop();
		}
	}

	/* Destroy时将数据库连接及TTS Engine设置为空
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (databaseHelper != null) {
			if (databaseHelper.getDb() != null) {
				databaseHelper.getDb().close();
			}
			databaseHelper.close();
			databaseHelper = null;
		}
		if (textToSpeech != null) {
			textToSpeech.shutdown();
		}
	}

	/**
	 * 搜索单词
	 */
	private void searchWord() {
		word = searchAutoCompleteTextView.getText().toString();
		if (word.length() > 0) {
			new SearchDicAsyncTask().execute(word);
		} else {
			makeToast("请输入单词");
		}
	}

	/**
	 * 显示Toast提示信息
	 * @param text 提示信息
	 */
	private void makeToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 显示Dialog提示信息
	 * @param text 提示信息
	 */
	private void showDialog(String text) {
		new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK", null).create().show();
	}

	/**
	 * 显示错误信息后退出
	 * @param text 错误信息
	 */
	private void showErrorDialog(String text) {
		new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		}).create().show();
	}

	/**
	 * 读取词典信息文件
	 * @return 格式化好的词典信息
	 */
	private String loadDictInfo() {
		// 读取词典信息
		FileAccessor ifoReader = null;
		String line;
		try {
			ifoReader = new FileAccessor(new File(ifoFileName), "r");

			while ((line = ifoReader.readLine()) != null) {
				String[] info = line.split("=");
				if (info[0].equals("bookname")) {
					info[1] = new String(info[1].getBytes("ISO-8859-1"), "UTF-8");
					bookName = info[1];
				} else if (info[0].equals("wordcount")) {
					wordCount = info[1];
				} else if (info[0].equals("idxfilesize")) {
					idxFileSize = info[1];
				}
			}
		} catch (FileNotFoundException ex) {
			showErrorDialog("不能读取词典文件，请检查SD卡后再试");
			ex.printStackTrace();
		} catch (IOException ex) {
			showErrorDialog("不能读取词典文件，请检查SD卡后再试");
			ex.printStackTrace();
		} finally {
			try {
				if (ifoReader != null) {
					ifoReader.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		dictInfo = "《" + bookName + "》词条[" + wordCount + "]";
		return dictInfo;
	}

	/**
	 * 展示选择词典对话框，如不选择一个词典，则无法进入软件
	 */
	private void showChooseDictDialog(boolean cancelable) {
		AlertDialog.Builder dictChooseBuilder = new AlertDialog.Builder(this);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.contains(".ifo");
			}
		};
		dictFilenames = dictPathFile.list(filter);
		if (dictFilenames.length > 0) {
			dictChooseBuilder.setItems(dictFilenames, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dictName = dictFilenames[which].substring(0, dictFilenames[which].indexOf(".ifo"));

					// 修改词典相关变量
					modifyDictVar(dictName);
					initDbFile();
					databaseHelper.initDb();

					if (new File(ifoFileName).isFile()) {
						setTitle(loadDictInfo());
					}

					if (new File(idxFileName).isFile() && dictFile.isFile()) {
						sharedPreferences.edit().putString(ZhuangDictActivity.PREFERENCES_DICT_NAME, dictName).commit();
						// 如果词典表不存在则创建
						// dbHelper.dropTable(tableName);
						if (!databaseHelper.isTableExist(tableName)) {
							databaseHelper.beginTransaction();
							databaseHelper.createTable(tableName);
							for (char c = 'a'; c <= 'z'; c++) {
								String temp = DatabaseHelper.transTableName(c);
								databaseHelper.createTable(temp);
							}
							databaseHelper.setTransactionSuccessful();
							databaseHelper.endTransaction();
							new LoadDictIndexAsyncTask().execute(dictName);
						}
					} else {
						showDialog(getResources().getString(R.string.choose_dict_content));
					}
				}
			});
			dictChooseBuilder.setCancelable(cancelable).setTitle(getResources().getString(R.string.choose_dict_title))
					.create().show();
		} else {
			dictChooseBuilder.setCancelable(cancelable).setMessage(
					getResources().getString(R.string.choose_dict_content)).create().show();
		}
	}

	/**
	 * 修改词典相关变量
	 * 
	 * @param dictName
	 */
	protected void modifyDictVar(String dictName) {
		tableName = DatabaseHelper.transTableName(GB2Alpha.DEFAULT_CHAR);

		ifoFileName = DICT_PATH + File.separator + dictName + ".ifo";
		idxFileName = DICT_PATH + File.separator + dictName + ".idx";
		dictFileName = DICT_PATH + File.separator + dictName + ".dict";
		dbFileName = DICT_PATH + File.separator + dictName + ".db";

		dictFile = new File(dictFileName);
		if (!dictFile.exists()) {
			dictFileName = DICT_PATH + File.separator + dictName + ".dict.dz";
			dictFile = new File(dictFileName);
			if (dictFile.exists()) {// 使用dz压缩文件
				isDZDict = true;
			}
		} else {
			isDZDict = false;
		}
	}

	/**
	 * 如果数据库文件不存在则创建
	 */
	private void initDbFile() {
		dbFile = new File(dbFileName);
		if (!dbFile.exists()) {
			try {
				dbFile.createNewFile();
			} catch (IOException e) {// 如果不能创建数据库文件则退出
				showErrorDialog("不能创建数据库文件，请检查SD卡空间后再试");
				e.printStackTrace();
			}
		}
	}

	/**
	 * 更新历史记录
	 * 
	 * @param word
	 * @param result
	 */
	private void updateHistory(String word, String result) {
		if (historyIndex + 1 == HISTORY_NUM) {// 所有数据左移
			for (int i = 0; ++i < HISTORY_NUM;) {
				historyWord[i - 1] = historyWord[i];
				historyResult[i - 1] = historyResult[i];
			}
		} else {
			historyIndex++;
		}
		historyWord[historyIndex] = word;
		historyResult[historyIndex] = result;
		// 清理之后的记录
		if (historyIndex > 1 && historyIndex < HISTORY_NUM) {
			for (int i = historyIndex; ++i < HISTORY_NUM;) {
				historyWord[i] = null;
				historyResult[i] = null;
			}
		}
	}

	/**
	 * 查询历史记录
	 * 
	 * @param isPre 是否向前查询
	 * @return 如果没有历史记录则返回False，由系统处理返回键事件
	 */
	private boolean loadHistory(boolean isPre) {
		if (isPre) {
			if (historyIndex > 0) {
				historyIndex--;
			}
		} else {
			if (historyIndex < HISTORY_NUM - 1) {
				historyIndex++;
			}
		}
		if (historyWord[historyIndex] != null && historyResult[historyIndex] != null) {
			searchAutoCompleteTextView.setText(historyWord[historyIndex]);
			searchAutoCompleteTextView.setSelection(historyWord[historyIndex].length());// 将光标移至文字末尾
			searchResultWebView.loadDataWithBaseURL(null, historyResult[historyIndex], "text/html", ENCODING, null);
			return true;
		}
		return false;
	}

	/**
	 * 字典索引读取AsyncTask类
	 * 
	 * @author wangxinxuan
	 * 
	 */
	private class LoadDictIndexAsyncTask extends AsyncTask<String, Integer, String> {
		/* 
		 * 读取词典索引
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected String doInBackground(String... params) {

			long start = System.currentTimeMillis();
			BufferedRandomAccessFile reader = null;
			byte[] bytes = new byte[IDX_BUFFER_LENGTH];
			int currentPos = 0;
			int insertNum = 0;
			int totalNum = 0;
			try {
				reader = new BufferedRandomAccessFile(idxFileName, "r");
				// dbHelper.setLockingEnabled(false);
				databaseHelper.beginTransaction();

				while (reader.hasMore()) {
					reader.readFully(bytes, 0, bytes.length);
					int j = 0;
					boolean isWordPart = true;
					boolean isOffsetPart = false;
					boolean isSizePart = false;
					long offset = 0; // offset of a word in data file
					long size = 0; // size of word's defition
					int wordLength = 0; // the byte(s) length of a word

					for (int i = 0; i < bytes.length; i++) {
						if (isWordPart) {
							if (bytes[i] == 0) {
								wordLength = i;
								word = new String(bytes, j, i - j, "UTF-8");
								j = i;
								isWordPart = false;
								isOffsetPart = true;
							}
							continue;
						}
						if (isOffsetPart) {
							i += 3;
							j++; // skip the split token: '\0'
							if (i >= bytes.length) {
								i = bytes.length - 1;
							}
							offset = ByteConverter.unsigned4BytesToInt(bytes, j);
							j = i + 1;
							isOffsetPart = false;
							isSizePart = true;
							continue;
						}
						if (isSizePart) {
							i += 3;
							if (i >= bytes.length) {
								i = bytes.length - 1;
							}
							size = ByteConverter.unsigned4BytesToInt(bytes, j);
							j = i + 1;
							isSizePart = false;
							isWordPart = true;
						}

						// 获取ih
						if (word.length() > 0) {
							char key = GB2Alpha.Char2Alpha(word.charAt(0));
							if (currentChar != key) {
								currentChar = key;
								tableName = DatabaseHelper.transTableName(key);
							}
							if (insertHelperMap.get(key) == null) {
								insertHelper = new InsertHelper(databaseHelper.getDb(), tableName);
								insertHelperMap.put(key, insertHelper);
							} else {
								insertHelper = insertHelperMap.get(key);
							}

							// 写入数据库
							insertHelper.prepareForInsert();
							insertHelper.bind(wordIndex, word);
							insertHelper.bind(offsetIndex, offset);
							insertHelper.bind(sizeIndex, size);
							insertHelper.execute();

							// 刷新进度条
							totalNum++;
							if (++insertNum == INSERT_NUM) {
								insertNum = 0;
								publishProgress(totalNum);
							}
						}
						// skip current index entry
						int indexSize = wordLength + 1 + 4 + 4;
						reader.seek(indexSize + currentPos);
						currentPos += indexSize;
						break;
					}
				}
				reader.close();
				publishProgress(totalNum);
				for (Character c : insertHelperMap.keySet()) {
					databaseHelper.createIndex(c);
				}
				databaseHelper.setTransactionSuccessful();
			} catch (FileNotFoundException ffe) {
				makeToast("对不起，没有找到索引文件：" + idxFileName);
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				databaseHelper.endTransaction();
				// dbHelper.setLockingEnabled(true);
				try {
					if (reader != null)
						reader.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			long end1 = System.currentTimeMillis();
			return "共导入词条[" + totalNum + "]耗时[" + (end1 - start) + "]毫秒";
		}

		/* 初始化进度条
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			idxImportProgressDialog = new ProgressDialog(ZhuangDictActivity.this);
			idxImportProgressDialog.setCancelable(false);
			idxImportProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			idxImportProgressDialog.setMax(Integer.valueOf(wordCount));
			idxImportProgressDialog.setMessage("《" + bookName + "》\n读入索引文件[" + idxFileSize
					+ "]比特\n因为要创建查询索引，以便查询速度更快，所以此过程时间稍长，请耐心等待");
			idxImportProgressDialog.show();
		}

		/* 索引读入完成后，显示Dialog展示相关信息
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String text) {
			idxImportProgressDialog.dismiss();
			showDialog("《" + bookName + "》\n读入索引文件[" + idxFileSize + "]比特\n" + text);
		}

		/* 更新进度条
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			idxImportProgressDialog.setProgress(values[0]);
		}

	}

	/**
	 * 字典查询AsyncTask类
	 * 
	 * @author wangxinxuan
	 * 
	 */
	private class SearchDicAsyncTask extends AsyncTask<String, Integer, String> {
		/**
		 * 0为没有查询结果；1为没有找到词典文件；2为查询异常
		 */
		private byte searchStatus;

		/* 查询单词
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected String doInBackground(String... params) {
			if (params[0].length() > 0) {
				word = params[0].trim();
				long[] index = null;

				FileAccessor in = null;
				DictZipInputStream din = null;
				try {

					// 查询出随机读取文件数值
					char key = GB2Alpha.Char2Alpha(word.charAt(0));
					tableName = DatabaseHelper.transTableName(key);

					index = databaseHelper.queryTable(tableName, word);

					if (index != null) {
						in = new FileAccessor(new File(dictFileName), "r");
						byte[] bytes = new byte[(int) index[1]];
						if (isDZDict) {
							din = new DictZipInputStream(in);
							DictZipHeader h = din.readHeader();
							int idx = (int) index[0] / h.getChunkLength();
							int off = (int) index[0] % h.getChunkLength();
							long pos = h.getOffsets()[idx];
							in.seek(pos);
							byte[] b = new byte[off + (int) index[1]];
							din.readFully(b);
							System.arraycopy(b, off, bytes, 0, (int) index[1]);
						} else {
							in.seek(index[0]);
							in.read(bytes);
						}

						wordDefinition = new String(bytes, "UTF-8");
					} else {
						searchStatus = 0;
						return null;
					}
				} catch (FileNotFoundException ffe) {
					searchStatus = 1;
					return null;
				} catch (IOException ex) {
					ex.printStackTrace();
					searchStatus = 2;
					return null;
				} finally {
					try {
						if (din != null)
							din.close();
						if (in != null)
							in.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				// long end1 = System.currentTimeMillis();
			}
			return wordDefinition;
		}

		/* 初始化进度条
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			searchProgressDialog.setMessage("正在查询，请稍侯");
			searchProgressDialog.show();
			//收键盘
			imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
		}

		/* 查询完成后渲染结果或展示错误信息
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String wordDefinition) {
			searchProgressDialog.dismiss();
			if (wordDefinition == null) {
				switch (searchStatus) {
				case 0:
					makeToast("[" + dictName + "]没有[" + word + "]词条");
					break;
				case 1:
					makeToast("对不起，没有找到词典文件[" + dictFileName + "]");
					break;
				case 2:
					makeToast("查询异常");
					break;
				}
			} else {
				// 更新历史记录
				updateHistory(word, wordDefinition);
				// 渲染WebView文本
				searchResultWebView.loadDataWithBaseURL(null, wordDefinition, "text/html", ENCODING, null);
				// 滚动至顶部
				searchResultWebView.scrollTo(0, 0);
			}
		}

	}

	/**
	 * 单词自动完成查询AsyncTask类
	 * 
	 * @author wangxinxuan
	 * 
	 */
	private class WordAutoComplite extends CursorAdapter {

		public WordAutoComplite(Context context, Cursor c) {
			super(context, c);
		}

		/* 在后台线程执行单词自动完成查询
		 * @see android.widget.CursorAdapter#runQueryOnBackgroundThread(java.lang.CharSequence)
		 */
		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence word) {
			return databaseHelper.queryAutoComplete(tableName, word.toString(), DEFAULT_AUTOCOMPLETE_LIMIT);
		}

		/* (non-Javadoc)
		 * @see android.widget.CursorAdapter#convertToString(android.database.Cursor)
		 */
		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(0);
		}

		/* (non-Javadoc)
		 * @see android.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater layoutInflater = LayoutInflater.from(context);
			return layoutInflater.inflate(android.R.layout.simple_dropdown_item_1line, null);
		}

		/* (non-Javadoc)
		 * @see android.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view).setText(cursor.getString(0));
		}
	}
}
