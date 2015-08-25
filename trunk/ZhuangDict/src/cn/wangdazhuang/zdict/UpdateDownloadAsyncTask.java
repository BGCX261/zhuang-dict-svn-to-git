package cn.wangdazhuang.zdict;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import cn.wangdazhuang.zdict.util.MD5File;

/**
 * 升级包下载并安装类
 * 
 * @author wangxinxuan
 * 
 */
@TargetApi(3)
public class UpdateDownloadAsyncTask extends AsyncTask<UpdateInfo, Integer, Boolean> {

	/**
	 * 存储卡根目录
	 */
	private File sdRoot = null;
	/**
	 * Context对象
	 */
	private Context context;
	/**
	 * 进度条
	 */
	private ProgressDialog progressDialog;
	/**
	 * 升级信息对象
	 */
	private UpdateInfo info = null;
	/**
	 * 是否可以下载
	 */
	private boolean canDownload = true;
	/**
	 * 是否可以升级
	 */
	private boolean canUpdate = false;
	/**
	 * 更新文件下载路径
	 */
	private File downFile;

	public UpdateDownloadAsyncTask(Context context, UpdateInfo info) {
		this.context = context;
		this.info = info;
	}

	@Override
	protected Boolean doInBackground(UpdateInfo... params) {
		if (canDownload) {
			// 校验参数
			if (info == null) {
				return false;
			}
			// 检查SD卡文件是否存在
			String fileName = info.url.substring(info.url.lastIndexOf("/"), info.url.length());
			String filePath = sdRoot + fileName;
			downFile = new File(filePath);

			HttpURLConnection httpConnection = null;
			InputStream is = null;
			OutputStream os = null;
			int downCount = 0;
			try {
				if (downFile.exists()) {// 如果存在则删除重新下载
					downFile.delete();
				}
				// 创建新文件
				if (!downFile.createNewFile()) {
					return false;
				}

				httpConnection = (HttpURLConnection) new URL(info.url).openConnection();
				// conn.setConnectTimeout(Constants.HTTP_CONN_TIMEOUT);
				if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					is = httpConnection.getInputStream();
					os = new FileOutputStream(downFile);
					byte buf[] = new byte[1024];
					int c;
					while ((c = is.read(buf)) != -1) {
						os.write(buf, 0, c);
						downCount += c;
						publishProgress(downCount);
					}
					os.flush();
//					publishProgress(downCount + c);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (httpConnection != null) {
					httpConnection.disconnect();
				}
				if (is != null && os != null) {
					try {
						is.close();
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				httpConnection = null;
				is = null;
				os = null;
			}
			// 校验文件大小及md5
			if (downFile.length() == info.size) {
				String md5 = MD5File.md5sum(filePath);
//				System.out.println(md5);
				if (md5.equals(info.md5)) {
					canUpdate = true;
				}
			}
		}
		return canUpdate;
	}

	@Override
	protected void onPreExecute() {
		// 判断是否有插入存储卡
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			sdRoot = Environment.getExternalStorageDirectory();
			// 检查容量是否可以下载新文件
			StatFs statFs = new StatFs(sdRoot.getPath());
			long availableSpare = statFs.getBlockSize() * ((long) statFs.getAvailableBlocks() - 4);
			if (availableSpare < info.size) {
				canDownload = false;
			}
		} else {
			canDownload = false;
		}

		if (canDownload) {
			// 初始化进度条
			progressDialog = new ProgressDialog(context);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle(context.getResources().getString(R.string.update_title));
			progressDialog.setMax(info.size);
			progressDialog.setMessage(info.message);
			progressDialog.show();
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		progressDialog.setProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		String ok = context.getResources().getString(R.string.confirm_update_yes);
		String text = null;
		if (!success) {// 下载失败
			text = context.getResources().getString(R.string.update_false);
			new AlertDialog.Builder(context).setMessage(text).setPositiveButton(ok, null).create().show();
		}
		if (canDownload) {
			// 安装新软件
			Intent intent = new Intent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setAction(android.content.Intent.ACTION_VIEW);

			/* 调用getMIMEType()来取得MimeType */
			String type = getMIMEType(downFile);
			/* 设置intent的file与MimeType */
			intent.setDataAndType(Uri.fromFile(downFile), type);
			context.startActivity(intent);
		} else {
			text = context.getResources().getString(R.string.update_cannot);
			new AlertDialog.Builder(context).setMessage(text).setPositiveButton(ok, null).create().show();
		}
	}

	/* 判断文件MimeType的method */
	private String getMIMEType(File f) {
		String type = "";
		String fName = f.getName();
		/* 取得扩展名 */
		String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();

		/* 依扩展名的类型决定MimeType */
		if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") || end.equals("xmf") || end.equals("ogg")
				|| end.equals("wav")) {
			type = "audio";
		} else if (end.equals("3gp") || end.equals("mp4")) {
			type = "video";
		} else if (end.equals("jpg") || end.equals("gif") || end.equals("png") || end.equals("jpeg")
				|| end.equals("bmp")) {
			type = "image";
		} else if (end.equals("apk")) {
			/* android.permission.INSTALL_PACKAGES */
			type = "application/vnd.android.package-archive";
		} else {
			type = "*";
		}
		/* 如果无法直接打开，就跳出软件列表给用户选择 */
		if (end.equals("apk")) {
		} else {
			type += "/*";
		}
		return type;
	}
}