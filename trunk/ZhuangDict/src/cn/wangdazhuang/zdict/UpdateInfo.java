package cn.wangdazhuang.zdict;


/**
 * 升级信息类
 * 
 * @author wangxinxuan
 * 
 */
public class UpdateInfo {
	// 版本号
	public String ver;
	// 下载APK文件路径
	public String url;
	// 是否需要强制升级
	public boolean force;
	// 文件大小，单位为byte
	public int size;
	// 文件md5值
	public String md5;
	// 升级信息
	public String message;
	
	@Override
	public String toString() {
		return "UpdateInfo [ver=" + ver + ", url=" + url + ", force=" + force + ", size=" + size + ", md5=" + md5
				+ ", content=" + message + "]";
	}
}
