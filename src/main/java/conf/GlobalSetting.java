package conf;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import conf.attribute.ArrayAttribute;
import conf.attribute.Attribute;
import conf.attribute.AttributeChainParser;
import conf.attribute.IAttribute;

/**
 * 全局配置文件的路径 ：classpath:global.js
 * 注意：相对的路径层次必须是这样：
 *   ..../classes/global.js
 *   ..../classes/xxx/yyy.js
 *   ..../lib/xxx.jar
 */
public final class GlobalSetting {

	static final String DefaultConfigFileName = "global.js";
	
	/**得到【fileName.js】的配置文件的路径。
	 * 处理了本类处于两种情况下（jar中、classes目录下），的两种情况：在这两种情况下自动找到配置的js文件。
	 * @param fileName：相对于[classes]的路径；基于文件系统的路径表示方法，最好是用[/]作为分隔符。
	 * 
	 * 要小心了：千万不能以[/]或者[\]开头：【/xxx.js】，【\xxx.js】不然就会变成类似于这样：【e:\xxx.js】的样的路径了。
	 * 
	 * @return
	 */
	static Path configPath(final String fileName){
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
			final String s = GlobalSetting.class.getResource("").toString();
			if(s.toLowerCase().startsWith("jar")){
				final String jarFilePath = s.substring(isWindows? 10: 9, s.lastIndexOf("!"));
				//指定配置文件路径
				r = Paths.get(jarFilePath).getParent()/*lib*/.getParent().resolve("classes")/*classes*/.resolve(_fileName);
			}else{
				//【E:\eclipsews\workspace001\globalsetting\target\classes】
				final Path cp/*classpath:.*/ = Paths.get(GlobalSetting.class.getResource("/").toURI());
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
		
		System.out.println(getByPath("another.js", "xx.mao[0]"));
		System.out.println(getByPath("another.js", "xx.mao[1]"));
		System.out.println(getByPath("another.js", "xx.mao[2]"));
		
		System.out.println(getByPath("another.js", "xx.mao[3].nangua[3][1]"));
		
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
	 * 按照 属性表达式，取出对应的属性值
	 * @param path --使用js中的属性引用表达方法。包括【.xx】、【[2333]】。目前不支持【['name']】这种引用属性的方法。
	 * @return
	 */
	public static Object getByPath(final String path){
		return getByPath(DefaultConfigFileName, path);
	}
	
	/**
	 * 按照 属性表达式，取出对应的属性值
	 * @param path --使用js中的属性引用表达方法。包括【.xx】、【[2333]】。目前不支持【['name']】这种引用属性的方法。
	 * @return
	 */
	public static String getStringByPath(final String path){
		final Object r = getByPath(DefaultConfigFileName, path);
		return r != null? r.toString(): null;
	}
	
	
	/**
	 * 按照 属性表达式，取出对应的属性值
	 * @param configFilePath　：相对于[classes]的路径；基于文件系统的路径表示方法，最好是用[/]作为分隔符，【要带.js结尾（注意大小 写）】
	 * @param path ：使用js中的属性引用表达方法。包括【.xx】、【[2333]】。目前不支持【['name']】这种引用属性的方法。
	 * @return
	 */
	public static Object getByPath(final String configFilePath, final String path){
		
		
//		try {
//			//启动js引擎
//			final ScriptEngineManager sem = new ScriptEngineManager();
//			final ScriptEngine engine = sem.getEngineByName("javascript");
//		    final String objStr = Files.toString(configPath(configFilePath).toFile(), Charsets.UTF_8);
//		    
//		    final StringBuilder sb = new StringBuilder("var globalSetting = ").append(objStr).append(";");
//		    sb.append("globalSetting.").append(path).append(";");
//		    //把配置对象字面量（js），转成json格式字符串出来
//		    final String str = (String)engine.eval(sb.toString());
//		    //然后，再转成java中的LinkedHashMap
//		    final Map<String,Object> map = new ObjectMapper().readValue(str, Map.class);
////		    logger.debug("global.js : "+ map);
//			return map;
//		} catch (Exception e) {
//			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", configFilePath) , e);
//		}
		
		
		
		
		try {
			final Map<String, Object > root  = get(configFilePath);
//			Object tmp = root;
//			for(final String key: Splitter.on('.').omitEmptyStrings().trimResults().split(Strings.nullToEmpty(path))){
//				if(tmp instanceof Map){
//					final Map map = (Map)tmp;
//					tmp = map.get(key);
//				}else{
//					throw new IllegalArgumentException(String.format("path=[%s], 太长了，或者是路径不对？", path));
//				}
//			}
//			return tmp;
			
			
			return parsePath(root, path);
		} catch (Exception e) {
			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", configFilePath) , e);
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
			throw new IllegalStateException(String.format("读取配置文件=[%s]失败", configFilePath) , e);
		}
	}
	
	
	
	/**
	 * 目前只能解析下面几种情况的js属性：
	 * 1. '.' [-a-zA-Z0-9_]+
	 * 2. '[' [0-9]+ ']'
	 * @param objFromJsObject
	 * @param path
	 * @return
	 */
	public static Object parsePath(final Map<String, Object> objFromJsObject, String path){
		if(objFromJsObject == null || Strings.isNullOrEmpty(path)){
			throw new IllegalArgumentException();
		}
		//js对象字面量的第一个属性引用，一定不是数组，一定是普通的属性
		path = "." + path.trim();
		final List<IAttribute> attList = AttributeChainParser.parse(path);
		Object currentObj = objFromJsObject;
		for(final IAttribute attribute: attList){
			if(attribute instanceof Attribute){
				final Attribute att = (Attribute)attribute;
				if(currentObj instanceof Map){
					currentObj = ((Map)currentObj).get(att.name);
				}
			}else if(attribute instanceof ArrayAttribute){
				final ArrayAttribute att = (ArrayAttribute)attribute;
				if(currentObj instanceof List){
					currentObj = ((List)currentObj).get(att.index);
				}
			}else{
				throw new IllegalStateException();
			}
		}
		return currentObj;
	}
	
	
	
	static boolean isWindows(){
		final String os = System.getProperty("os.name");  
		if(os.toLowerCase().startsWith("win")){  
		  return true;
		}
		return false;
	}
}
