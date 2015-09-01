package my.relativepath;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

/**
 * 按相对路径搜索
 */
public class RelativeFileTraveller {

	public final String suffix;
	public final Path rootPath;
	/**
	 * 有序的，顺序是按路径来排列。深度优先；同级的话，就按照 字符串的顺序来比较。
	 * 有了这个，就不再需要另外再建立一个关于文件的层次数据结构。
	 */
	private final SortedSet<RelativeFile> targetFiles = new TreeSet<>(comparator);

	/**
	 * @param root 必须是一个目录
	 * @param suffix 后缀名，例如【.js】
	 */
	public RelativeFileTraveller(final Path root, final String suffix) {
		if(root == null){
			throw new IllegalArgumentException("root is null");
		}
		final Path realRoot = root.normalize().toAbsolutePath();
		if(!realRoot.toFile().exists() || !realRoot.toFile().isDirectory()){
			throw new IllegalArgumentException(String.format("root=[%s] not exist or not a directory", root));
		}
		if(Strings.isNullOrEmpty(suffix) || !suffix.startsWith(".")){
			throw new IllegalArgumentException(String.format("suffix=[%s] 不正确", suffix));
		}
		this.suffix = suffix;
		this.rootPath = realRoot;
		init0(rootPath);
		//debug
//		System.out.println(targetFiles);
	}

	public Path search(final String relativePath){
		if(!RelativeFile.mayBeRelativePath(relativePath)){
			throw new IllegalArgumentException(String.format("path=[%s] is not a relative path", relativePath));
		}
		final RelativeFile target = new RelativeFile(rootPath.resolve(relativePath + suffix).normalize().toAbsolutePath(), rootPath, suffix);
		if (targetFiles.contains(target)){
			return target.filePath;
		}
		return null;
	}
	
	
	/**
	 * @param visitor
	 */
	public void traverseLeaves(final Visitor visitor){
		for(final RelativeFile i: targetFiles){
			if(i.isLeaf){
				visitor.visit(i);
			}
		}
	}
	
	/**
	 * @param visitor
	 * @param strictlyLessThan 必须严格大于
	 */
	public void headTraverseLeaves(final Visitor visitor, final String strictlyLessThan){
		if(!RelativeFile.mayBeRelativePath(strictlyLessThan)){
			throw new IllegalArgumentException(String.format("path=[%s] is not a relative path", strictlyLessThan));
		}
		for(final RelativeFile i: targetFiles.headSet(new RelativeFile(rootPath.resolve(strictlyLessThan), rootPath, suffix))){
			if(i.isLeaf){
				visitor.visit(i);
			}
		}
	}
	
	
	
	/**
	 * @param visitor
	 * @param greaterThan 可以包含equals的情况
	 */
	public void tailTraverseLeaves(final Visitor visitor, final String greaterThan){
		if(!RelativeFile.mayBeRelativePath(greaterThan)){
			throw new IllegalArgumentException(String.format("path=[%s] is not a relative path", greaterThan));
		}
		for(final RelativeFile i: targetFiles.tailSet(new RelativeFile(rootPath.resolve(greaterThan), rootPath, suffix))){
			if(i.isLeaf){
				visitor.visit(i);
			}
		}
	}
	
	
	
	@FunctionalInterface
	public static interface Visitor{
		public void visit(RelativeFile path);
	}
	
	void init0(final Path path){
		if(!path.toFile().isDirectory()){
			throw new IllegalArgumentException(String.format("path=[%s] 必须是目录", path));
		}
		for(final File f: path.toFile().listFiles()){
			if(f.isDirectory()){
				init0(f.toPath());
				final RelativeFile rf = new RelativeFile(f.toPath(), rootPath, suffix);
				targetFiles.add(rf);
				continue;
			}
			if(f.getName().endsWith(suffix)){
				final RelativeFile rf = new RelativeFile(f.toPath(), rootPath, suffix);
				targetFiles.add(rf);
				continue;
			}
		}
	}
	
	
	/**
	 * 比较器，按目录深度优先（将之是文件名字的名字的字符串比较）
	 * 因为只限这里用，不需要再判断rootPath、suffix这些是否是真的一样了。
	 */
	private static final Comparator<RelativeFile> comparator = new Comparator<RelativeFile>(){
		@Override
		public int compare(final RelativeFile o1, final RelativeFile o2) {
			 return ComparisonChain.start().compare(o2.pathNodeList.size(), o1.pathNodeList.size()).compare(o1.path, o2.path).result();
		}
	};
	
}
