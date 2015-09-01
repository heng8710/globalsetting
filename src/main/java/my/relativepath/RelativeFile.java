package my.relativepath;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;


/**
 *	快速切换：【相对路径】<---->【绝对路径】
 *  包含文件夹、和带某种{@link #suffix}后缀的文件
 */
public class RelativeFile {
	
	/**
	 * 后缀，要带点【.】，比如【.js】
	 * 如果是目录就没有，用null表示
	 */
	public final String suffix;
	
	/**
	 * 是否是叶子节点，这由{@link #filePath}决定，如果是文件就是true，如果是文件夹就是false
	 */
	public final boolean isLeaf;
	
	/**
	 * 相对路径，写法类似于android 里面的values引用。【a/b/c】 <br/>
	 * 不带 {@link #suffix}后缀作为结尾（建议小写），但是要求文件必须以{@link #suffix}作为后缀。 <br/>
	 * 可以作为key来使用。 <br/>
	 * 示例：【{@link #path}=.../a/b/c{@link #suffix}】对应于：【a/b/c】 <br/>
	 */
	public final String path;
	
	/**
	 * {@link #path}的每一个节点
	 */
	public final List<String> pathNodeList;
	
	
	/**
	 * {@link #path}的真实路径
	 */
	public final Path filePath;
	
	/**
	 * 所根据的目录。{@link #filePath} = {@link #root} + '/' + {@link #path} + {@link #suffix}
	 * 
	 */
	public final Path root;
	
	
	

	/**
	 * @param filePath ： 要代表的文件路径，绝对路径。
	 * @param root ：相对于的路径（目录），绝对路径。
	 * @param suffix ：　代表的文件的后缀名
	 */
	public RelativeFile(final Path filePath,final Path root, final String suffix) {
		if(filePath == null || !filePath.toFile().exists()){
			throw new IllegalArgumentException(String.format("filePath=[%s] 不存在", filePath));
		}
		if(root == null || !root.toFile().exists() || !root.toFile().isDirectory()){
			throw new IllegalArgumentException(String.format("root目录=[%s] 不存在或者不是目录", root));
		}
		final Path realFilePath = filePath.normalize().toAbsolutePath();
		final Path realRoot = root.normalize().toAbsolutePath();
		if(!realFilePath.startsWith(realRoot)){
			throw new IllegalArgumentException(String.format("filePath=[%s] 并不在root=[%s]之下", filePath, root));
		}
		this.filePath = realFilePath;
		this.root = realRoot;
		if(realFilePath.toFile().isDirectory()){
//			throw new IllegalArgumentException(String.format("filePath=[%s] 的文件类型不对", filePath));
			this.suffix = null;
			this.isLeaf = false;
		}else{
			if(!realFilePath.toFile().getName().endsWith(suffix)){
				throw new IllegalArgumentException(String.format("filePath=[%s] 的文件类型可能不对，后缀名不是[%s]", filePath, suffix));
			}
			if(Strings.isNullOrEmpty(suffix) || !suffix.startsWith(".")){
				throw new IllegalArgumentException(String.format("suffix=[%s] 不正确", suffix));
			}
			this.suffix = suffix;
			this.isLeaf = true;
		}
		final LinkedList<Path> list = Lists.newLinkedList();
		for(Path p=realFilePath; !Objects.equal(realRoot, p) && p.startsWith(realRoot)/*如果一样，也是true*/; p=p.getParent().normalize().toAbsolutePath()){
			list.addFirst(p);
		}
		final StringBuilder sb = new StringBuilder();
		final ImmutableList.Builder<String> lb = ImmutableList.builder();
		for(final Path p: list){
			if(Objects.equal(realFilePath, p)){
				final File f = p.toFile();
				final String nameWithoutSuffix = (!f.isDirectory())?Files.getNameWithoutExtension(f.getPath()): f.getName();
				lb.add(nameWithoutSuffix);
				sb.append(nameWithoutSuffix);
			}else{
				final String pathNodeName = p.toFile().getName();
				lb.add(pathNodeName);
				sb.append(pathNodeName).append("/");
			}
		}
		this.pathNodeList = lb.build();
		this.path = sb.toString();
	}


	



	/* 
	 * 为了让这种用相对路径的有序。
	 * （就用相对路径{@link #path}的字符串比较）
	 */
//	@Override
//	public int compareTo(final RelativeFile that) {
//		return ComparisonChain.start().compare(that.pathNodeList.size(), this.pathNodeList.size()).compare(this.path, that.path).result();
//	}

	
	
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("suffix", suffix)
				.add("isLeaf", isLeaf).add("path", path)
				.add("pathNodeList", pathNodeList).add("filePath", filePath)
				.add("root", root).toString();
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(suffix, path, root);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof RelativeFile) {
			RelativeFile that = (RelativeFile) object;
			return Objects.equal(this.suffix, that.suffix)
					&& Objects.equal(this.path, that.path)
					&& Objects.equal(this.root, that.root);
		}
		return false;
	}






	public static boolean mayBeRelativePath(final String path){
		if(Objects.equal("", path)){
			return true;
		}
		if(path == null || Objects.equal("", path.trim())){
			throw new IllegalArgumentException(String.format("path=[%s]不合法", path));
		}
		return path.indexOf(":") == -1 && !path.startsWith("/");
	}
	
	
//	@FunctionalInterface
//	public static interface Suffix{
//		public String get();
//	} 
}
