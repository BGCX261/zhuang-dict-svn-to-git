package cn.wangdazhuang.zdict;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;

/**
 * 获取升级信息的升级查询管理类
 * 
 * @param 传入两个参数
 *            ，第一个为xml文件的url，第二个为版本号，如果版本号不一致则要显示是否更新提示框
 * @author wangxinxuan
 * 
 */
public class UpdateInfoAsyncTask extends AsyncTask<String, Integer, UpdateInfo> {

	/**
	 * Context对象
	 */
	private Context context;
	/**
	 * xml文件url
	 */
	private String url;
	/**
	 * 版本号
	 */
	private int ver;
	/**
	 * 升级信息对象
	 */
	private UpdateInfo info = null;
	/**
	 * 更新信息
	 */
	private SharedPreferences config;
	/**
	 * 配置文件中的版本号
	 */
	private int configVer;
	/**
	 * 配置文件中的最后更新日期
	 */
	private String lastCheck;
	/**
	 * 当前更新日期
	 */
	private String todayCheck;

	public UpdateInfoAsyncTask(Context context) {
		this.context = context;
	}

	@Override
	protected UpdateInfo doInBackground(String... params) {
		// 校验参数
		if (params == null || params.length < 2 || params[0] == null || params[1] == null) {
			return null;
		} else {
			url = params[0];
			ver = Integer.valueOf(params[1]);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		todayCheck = sdf.format(new Date());
		// 检查是否是最新版本
		config = context.getSharedPreferences(ZhuangDictActivity.UPDATE_SHAREDPREFERENCES_FILE, Context.MODE_PRIVATE);
		configVer = config.getInt("ver", ZhuangDictActivity.versionCode);
		lastCheck = config.getString("last", null);
		if (lastCheck == null) { // 如果还没有创建则设置默认值
			// 设置版本号及检查标志
			config.edit().putInt("ver", ver).commit();
			config.edit().putString("last", todayCheck).commit();
		} else {
			// 如果版本号需要更新，则要更新config
			if (ver > configVer) {
				config.edit().putInt("ver", ver).commit();
				configVer = ver;
			}
			// 检查今天是否已经更新过，如果已经更新过则退出
			if (todayCheck.equals(lastCheck)) {
				return null;
			}
		}

		HttpURLConnection httpConnection = null;
		InputStream is = null;
		try {
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			// conn.setConnectTimeout(Constants.HTTP_CONN_TIMEOUT);
			if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				info = new UpdateInfo();
				is = httpConnection.getInputStream();
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document dom = db.parse(is);
				Element root = dom.getDocumentElement();
				NodeList jrj = root.getElementsByTagName("jrj");
				if (jrj != null && jrj.getLength() > 0) {
					Element entry = (Element) jrj.item(0);
					info.ver = entry.getAttribute("ver");
					info.url = entry.getAttribute("url");
					info.force = entry.getAttribute("force").equals("true") ? true : false;
					info.size = Integer.valueOf(entry.getAttribute("size"));
					info.md5 = entry.getAttribute("md5");
					info.message = entry.getFirstChild().getNodeValue();
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			httpConnection = null;
			is = null;
		}
		return info;
	}

	@Override
	protected void onPostExecute(UpdateInfo result) {
		if (result != null && result.ver != null) {
			if (!result.ver.equals(configVer)) {// 版本号不一致则要显示是否升级提示框
				if (!result.force && !todayCheck.equals(lastCheck)) {// 如果不需要强制升级，并且更新日期不同，则更新config
					config.edit().putString("last", todayCheck).commit();
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(context.getResources().getString(R.string.confirm_update_title))
						.setMessage(result.message)
						.setCancelable(false)
						.setPositiveButton(context.getResources().getString(R.string.confirm_update_yes),
								new UpdateOnClickListener())
						.setNegativeButton(context.getResources().getString(R.string.confirm_update_no),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
									}
								}).create().show();
			}
		}
	}

	/**
	 * 点击确定按钮则开启后台下载线程
	 * 
	 * @author wangxinxuan
	 * 
	 */
	private class UpdateOnClickListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			new UpdateDownloadAsyncTask(context, info).execute();
		}

	}
}
