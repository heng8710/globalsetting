package js;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import my.relativepath.RelativeFile;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class RelativeFileSearcher {

	public final String suffix;
	public final Path rootPath;
	private final SearchNode rootSearchNode;//遍历的时候使用
	private final SortedMap<String, RelativeFile> targetFiles = new TreeMap<>();

	/**
	 * @param root 必须是一个目录
	 * @param suffix 后缀名，例如【.js】
	 */
	public RelativeFileSearcher(final Path root, final String suffix) {
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
		this.rootSearchNode = new SearchNode(rootPath);
	}

	public Path search(final String relativePath){
		final RelativeFile file = targetFiles.get(relativePath);
		return file.filePath;
	}
	
	
	public void depthFirstTraverse(final Visitor visitor){
		depthFirstTraverse0(rootSearchNode, visitor);
	}
	
	private void depthFirstTraverse0(final SearchNode currentSearchNode/*肯定是目录*/, final Visitor visitor){
//		if(!currentSearchNode.mayHasChildren()){
//		}
		final List<RelativeFile> files = Lists.newLinkedList();
		for(final SearchNode childNode: currentSearchNode.childrenSearchNode){
			if(!childNode.isDirectory()){
				files.add(new RelativeFile(childNode.realFilePath, rootPath, suffix));
				continue;
			}
			depthFirstTraverse0(childNode, visitor);
		}
		for(final RelativeFile file: files){
			visitor.visit(file);
		}
	}
	
	public static interface Visitor{
		public void visit(RelativeFile path);
	}
	
	/**
	 * 必须是目录
 	 */
	private class SearchNode{
		final String nodeName;
		final Path realFilePath;
		private final SortedSet<SearchNode> childrenSearchNode = new TreeSet<>();//有序
		private final SortedSet<RelativeFile> files = new TreeSet<>();//有序
		
		private SearchNode(final Path path) {
			final Path realFilePath = path.normalize().toAbsolutePath();
			this.realFilePath = realFilePath;
			this.nodeName = realFilePath.toFile().getName();
			if(!isDirectory()){
				throw new IllegalArgumentException(String.format("path=[%s] 必须是目录", path));
			}
			for(final File f: realFilePath.toFile().listFiles()){
				if(f.isDirectory()){
					childrenSearchNode.add(new SearchNode(f.toPath()));
				}else{
					if(f.getName().endsWith(suffix)){
						final RelativeFile rf = new RelativeFile(f.toPath(), rootPath, suffix);
						files.add(rf);
						targetFiles.put(rf.path, rf);
					}
				}
			}
		}
		
		/**
		 * 判断 是否是叶子
		 * @return
		 */
		boolean isDirectory(){
			final File f = realFilePath.toFile();
			return f.isDirectory();
		}
		
		boolean hasChildren(){
			return isDirectory()? childrenSearchNode.size() > 0 || files.size() > 0: false;
		}
	} 
	
}
