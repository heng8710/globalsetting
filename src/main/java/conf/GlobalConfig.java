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
 *   ..../classes/xxx/yyy.js
 *   ..../lib/xxx.jar
 */
public class GlobalConfig {

	static final String DefaultConfigFileName = "global.js";
	
//	static Path configPath(){
//		return configPath(DefaultConfigFileName);
//	}
	
	/**得到【fileName.js】的配置文件的路径。
	 * 处理了本类处于两种情况下（jar中、classes目录下），的两种情况：在这两种情况下自动找到配置的js文件。
	 * @param fileName：相对于[classes]的路径；基于文件系统的路径表示方法，最好是用[/]作为分隔符。
	 * 
	 * 要小心了：千万不能以[/]或者[\]开头：【/xxx.js】，【\xxx.js】不然就会变成类似于这样：【e:\xxx.js】的样的路径了。
	 * 
	 * @return
	 */
	static Path configPath(final String fileName/*without [.js] subfix*/){
		final String _fileName = (!Strings.isNullOrEmpty(fileName))? fileName.trim(): fileName;
		if(Strings.isNullOrEmpty(_fileName) || _fileName.startsWith("/") || _fileName.startsWith("\\")){
			throw new IllegalArgumentException(String.format("fileName=[%s]不对", fileName));
		}
		try {
			final Path r;
			final boolean isWindows = isWindows();
			//如果是在jar中就是下面的路径：
			//【jar:file:/F:/ftp/dailijob/lib/globalsetting-1.0.0.jar!/conf/】
			//【jar:file:/data/application/dailijob/lib/globalsetting-1.0.0.jar!/conf/】
			final String s = GlobalConfig.class.getResource("").toString();
			if(s.toLowerCase().startsWith("jar")){
				final String jarFilePath = s.substring(isWindows? 10: 9, s.lastIndexOf("!"));
				//指定配置文件路径
				r = Paths.get(jarFilePath).getParent()/*lib*/.getParent().resolve("classes")/*classes*/.resolve(_fileName);
			}else{
				//【E:\eclipsews\workspace001\globalsetting\target\classes】
				final Path cp/*classpath:.*/ = Paths.get(GlobalConfig.class.getResource("/").toURI());
				r = cp.resolve(_fileName);//?多层子路径
			}
//			System.out.println("path="+r);
			return r;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("cannot find the path of [%s.js]", _fileName), e);
		}
	}
	
	public static void main(String...strings) throws URISyntaxException{
		System.out.println(get());
		System.out.println(getByPath("a"));
		System.out.println(getByPath("mail.host"));
		
		System.out.println(get("another.js"));
		System.out.println(getByPath("another.js", "a"));
		System.out.println(getByPath("another.js", "mail.host"));
		
		System.out.println(get("noexit.js"));
		System.out.println(getByPath("noexit.js", "a"));
		System.out.println(getByPath("noexit.js", "mail.host"));
		
//		System.out.println(get("/another.js"));//不要以[/开头]
//		System.out.println(getByPath("/another.js", "a"));
//		System.out.println(getByPath("/another.js", "mail.host"));
		
//		System.out.println(get("\\another.js"));//不要以[\\开头]
//		System.out.println(getByPath("\\another.js", "a"));
//		System.out.println(getByPath("\\another.js", "mail.host"));
		
		
//		Path p = Paths.get("d:/").resolve("ssha");
//		System.out.println(p);
		
		
		
//		Path p2 = Paths.get(GlobalConfig.class.getResource("/").toURI());
//		System.out.println(p2);
//		
//		
//		Path p3 = Paths.get(GlobalConfig.class.getResource("").toURI());
//		System.out.println(p3);
//		
//		
//		Path p4 = Paths.get(GlobalConfig.class.getResource(".").toURI());
//		System.out.println(p4);
		
	}
	
	/**
	 * 暂时不支持数组表达式： 因为暂时看起来好像没有什么必要
	 * @param path --按点.分隔，比如：【mail.host】（这也意味阒所有key不能有点号在中间）
	 * @return
	 */
	public static Object getByPath(final String path){
		return getByPath(DefaultConfigFileName, path);
	}
	
	/**
	 * @param configFilePath　：相对于[classes]的路径；基于文件系统的路径表示方法，最好是用[/]作为分隔符，【要带.js结尾（注意大小 写）】
	 * @param path ：用[.]分隔，类型于对象的属性值引用表示方法。
	 * @return
	 */
	public static Object getByPath(final String configFilePath, final String path){
		try {
			final Map<String, Object > root  = get(configFilePath);
			Object tmp = root;
			for(final String key: Splitter.on('.').omitEmptyStrings().trimResults().split(Strings.nullToEmpty(path))){
				if(tmp instanceof Map){
					final Map map = (Map)tmp;
					tmp = map.get(key);
				}else{
					throw new IllegalArgumentException(String.format("path=[%s], 太长了，或者是路径不对？", path));
				}
			}
			return tmp;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", DefaultConfigFileName) , e);
		}
	}
	
	
	
	public static Map<String,Object> get( ){
		return get(DefaultConfigFileName);
	}
	
	/**
	 * @param configFilePath：相对于[classes]的路径；基于文件系统的路径表示方法，最好是用[/]作为分隔符，【要带.js结尾（注意大小 写）】
	 * @return
	 */
	public static Map<String,Object> get(final String configFilePath){
		try {
			//启动js引擎
			final ScriptEngineManager sem = new ScriptEngineManager();
			final ScriptEngine engine = sem.getEngineByName("javascript");
		    final String objStr = Files.toString(configPath(configFilePath).toFile(), Charsets.UTF_8);
		    //把配置对象字面量（js），转成json格式字符串出来
		    final String str = (String)engine.eval("JSON.stringify("+objStr+");");
		    //然后，再转成java中的LinkedHashMap
		    final Map<String,Object> map = new ObjectMapper().readValue(str, Map.class);
//		    logger.debug("global.js : "+ map);
			return map;
		} catch (Exception e) {
			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", DefaultConfigFileName) , e);
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
