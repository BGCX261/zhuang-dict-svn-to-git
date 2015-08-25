package cn.wangdazhuang.zdict;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 操作数据库的核心类
 * 
 * @author wangxinxuan
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {
	/**
	 * 排序索引字段
	 */
	public static final String INDEX = "_index";
	/**
	 * 词典表名
	 */
	public static final String DICT_T = "dict_t";
	/**
	 * 词典名称
	 */
	public static final String DICT_NAME = "_dict_name";
	/**
	 * 词典包含单词数量
	 */
	public static final String WORD_COUNT = "_word_count";
	/**
	 * 词典索引文件大小
	 */
	public static final String IDX_FILE_SIZE = "_idx_file_size";

	/**
	 * 单词字段
	 */
	public static final String WORD = "_word";
	/**
	 * 单词offset字段
	 */
	public static final String OFFSET = "_offset";
	/**
	 * 单词size字段
	 */
	public static final String SIZE = "_size";

	/**
	 * 数据库可写实例
	 */
	private SQLiteDatabase db = null;

	/**
	 * 得到数据库可写实例
	 */
	public SQLiteDatabase getDb() {
		return db;
	}

	/**
	 * 构造方法
	 * @param context
	 */
	public DatabaseHelper(Context context) {
		super(context, "zhuang", null, ZhuangDictActivity.versionCode);
//		db = getWritableDatabase();
//		db = SQLiteDatabase.openOrCreateDatabase(ZhuangDictActivity.dbFile, null);
	}
	
	/**
	 * 切换数据库
	 */
	public void initDb() {
		if (db != null) {
			db.close();
		}
		db = SQLiteDatabase.openOrCreateDatabase(ZhuangDictActivity.dbFile, null);
	}

	/**
	 * 删除表
	 * @param tableName 表名
	 */
	public void dropTable(String tableName) {
		db.execSQL("DROP TABLE IF EXISTS " + tableName);
	}
	
	/**
	 * 查询表是否存在
	 * @param tableName 表名
	 * @return 存在返回 true
	 */
	public boolean isTableExist(String tableName) {
		Cursor result = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';", null);
		boolean isTableExist = result.getCount() > 0;
		result.close();
		return isTableExist;
	}

	/**
	 * 创建词典索引表
	 * @param tableName 表名
	 */
	public void createTable(String tableName) {
		db.execSQL("CREATE TABLE " + tableName + " (" + WORD + " TEXT, " + OFFSET
				+ " INTEGER, " + SIZE + " INTEGER);");
	}

	/**
	 * 创建词典索引表索引
	 * @param key 表名前缀
	 */
	public void createIndex(char key) {
		String temp = transTableName(key);
		db.execSQL("CREATE INDEX IF NOT EXISTS '" + temp + INDEX + "' ON '" + temp + "' (" + WORD + ");");
	}

	/**
	 * 插入数据
	 * @param tableName 表名
	 * @param values 数据对象
	 */
	public void insertTable(String tableName, ContentValues values) {
		db.insert(tableName, null, values);
	}
	
	/**
	 * 查询词典索引数据
	 * @param tableName 表名
	 * @param word 查询单词
	 * @return offset 和 size 的 long 数组
	 */
	public long[] queryTable(String tableName, String word) {
		long[] index = null;
		Cursor result = db.query(tableName, null, WORD + "=? COLLATE NOCASE", new String[]{word}, null, null, null);
		result.moveToFirst();
		if (!result.isAfterLast()) {
			index = new long[2];
			index[0] = result.getLong(result.getColumnIndex(OFFSET));
			index[1] = result.getLong(result.getColumnIndex(SIZE));
		}
		result.close();
		return index;
	}
	
	/**
	 * 查询自动完成输入框需要的词典索引数据
	 * @param tableName 表名
	 * @param word 查询单词
	 * @param limit 如果为0则为不限制查询数量
	 * @return Cursor
	 */
	public Cursor queryAutoComplete(String tableName, String word, int limit) {
		String sql = "select " + WORD + " as _id from " + tableName + " where " + WORD + " like ? COLLATE NOCASE";
		// 如果限制查询数量
		if (limit > 0) {
			sql += " limit " + limit;
		}
		Cursor result = db.rawQuery(sql, new String[]{word + "%"});
		return result;
	}
	
	/**
	 * 查询所有词典索引数据
	 * @param tableName 表名
	 * @return String 数组
	 */
	public String[] queryAllAutoComplete(String tableName) {
		String[] rv = null;
		Cursor result = db.query(tableName, null, null, null, null, null, null);
		int count = result.getCount();
		if (count > 0) {
			rv = new String[count];
			for (int i = count; --i >= 0;) {
				result.moveToPosition(i);
				rv[i] = result.getString(0);
			}
		}
		result.close();
		return rv;
	}
	
	/**
	 * 转换词典名为表名
	 * @param key 表名前缀
	 * @return 表名
	 */
	public static String transTableName(char key) {
		return key + "_t";
	}
	
	/**
	 * 手动设置开始事务
	 */
	public void beginTransaction() {
		db.beginTransaction();
	}
	
	/**
	 * 手动设置事务处理成功，不设置会自动回滚不提交
	 */
	public void setTransactionSuccessful() {
		db.setTransactionSuccessful();
	}
	
	/**
	 * 手动设置结束事务
	 */
	public void endTransaction() {
		db.endTransaction();
	}
	
	/**
	 * 手动设置同步锁检查功能
	 * @param lockingEnabled 是否设置锁机制
	 */
	public void setLockingEnabled(boolean lockingEnabled) {
		db.setLockingEnabled(lockingEnabled);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// 创建词典表
		db.execSQL("CREATE TABLE " + DICT_T + " (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + DICT_NAME
				+ " TEXT, " + WORD_COUNT + " INTEGER, " + IDX_FILE_SIZE + " INTEGER, " + INDEX + " INTEGER);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// 删除所有表
		Cursor result = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null);
		// 删除所有表，包括临时表
		// Cursor result =
		// db.rawQuery("SELECT name FROM (SELECT * FROM sqlite_master UNION ALL SELECT * FROM sqlite_temp_master) WHERE type='table' ORDER BY name",
		// null);
		result.moveToFirst();
		while (!result.isAfterLast()) {
			dropTable(result.getString(result.getColumnIndex("name")));
			result.moveToNext();
		}
		result.close();
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
	}
}