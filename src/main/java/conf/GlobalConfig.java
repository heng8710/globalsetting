package conf;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.Files;

/**
 * 全局配置文件的路径 ：classpath:global.js
 * 注意：相对的路径层次必须是这样：
 *   ..../classes/global.js
 *   ..../lib/xxx.jar
 */
public class GlobalConfig {

	private static final String ConfigFileName = "global.js";
	public static final Path GlobalJsPath;
	static {
		try {
			final boolean isWindows = isWindows();
			//【jar:file:/F:/ftp/dailijob/lib/globalsetting-1.0.0.jar!/conf/】
			//【jar:file:/data/application/dailijob/lib/globalsetting-1.0.0.jar!/conf/】
			final String s = GlobalConfig.class.getResource("").toString();
			final String jarFilePath = s.substring(isWindows? 10: 9, s.lastIndexOf("!"));
			//指定配置文件路径
			GlobalJsPath = Paths.get(jarFilePath).getParent()/*lib*/.getParent().resolve("classes")/*classes*/.resolve(ConfigFileName);
//			GlobalJsPath = Paths.get(GlobalConfig.class.getResource("/").toURI()).resolve(ConfigFileName).toFile().getAbsolutePath();
		} catch (Exception e) {
			throw new IllegalStateException("cannot find the path of global.js");
		}
	}
	
	public static void main(String...strings){
		System.out.println(get());
		System.out.println(get("a"));
		System.out.println(get("mail.host"));
		
	}
	
	/**
	 * 暂时不支持数组表达式： 因为暂时看起来好像没有什么必要
	 * @param path --按点.分隔，比如：【mail.host】（这也意味阒所有key不能有点号在中间）
	 * @return
	 */
	public static Object get(final String path){
		try {
			final Map<String, Object > root  = get();
			Object tmp = root;
			for(final String key: Splitter.on('.').omitEmptyStrings().trimResults().split(Strings.nullToEmpty(path))){
				if(tmp instanceof Map){
					final Map map = (Map)tmp;
					tmp = map.get(key);
				}else{
					throw new IllegalArgumentException(String.format("path=[%s], 太长了？", path));
				}
			}
			return tmp;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", ConfigFileName) , e);
		}
	}
	
	public static Map<String,Object> get( ){
		try {
			//启动js引擎
			final ScriptEngineManager sem = new ScriptEngineManager();
			final ScriptEngine engine = sem.getEngineByName("javascript");
		    final String objStr = Files.toString(GlobalJsPath.toFile(), Charsets.UTF_8);
		    //把配置对象字面量（js），转成json格式字符串出来
		    final String str = (String)engine.eval("JSON.stringify("+objStr+");");
		    //然后，再转成java中的LinkedHashMap
		    final Map<String,Object> map = new ObjectMapper().readValue(str, Map.class);
//		    logger.debug("global.js : "+ map);
			return map;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", ConfigFileName) , e);
		}
	}
	
	
	static boolean isWindows(){
		final String os = System.getProperty("os.name");  
		if(os.toLowerCase().startsWith("win")){  
		  return true;
		}
		return false;
	}
}
