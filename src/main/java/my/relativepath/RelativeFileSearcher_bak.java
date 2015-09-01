package my.relativepath;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

public class RelativeFileSearcher_bak {

	public final String suffix;
	public final Path rootPath;
	private final SearchNode rootSearchNode;//遍历的时候使用
	private final SortedMap<String, RelativeFile> targetFiles = new TreeMap<>();

	/**
	 * @param root 必须是一个目录
	 * @param suffix 后缀名，例如【.js】
	 */
	public RelativeFileSearcher_bak(final Path root, final String suffix) {
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
		
		
		//debug
		System.out.println(targetFiles);
	}

	public Path search(final String relativePath){
		final RelativeFile file = targetFiles.get(relativePath);
		return file.filePath;
	}
	
	
	public void depthFirstTraverse(final Visitor visitor){
		depthFirstTraverse0(rootSearchNode, visitor);
	}
	
	private void depthFirstTraverse0(final SearchNode currentSearchNode/*肯定是目录*/, final Visitor visitor){
//		for(final SearchNode childNode: currentSearchNode.childrenSearchNode){
//			depthFirstTraverse0(childNode, visitor);
//		}
//		for(final RelativeFile file: currentSearchNode.files){
//			visitor.visit(file);
//		}
		
		for(final Entry<String, RelativeFile> entry: targetFiles.entrySet()){
			visitor.visit(entry.getValue());
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
		private final SortedSet<SearchNode> childrenSearchNode = new TreeSet<>(new Comparator<SearchNode>(){
			@Override
			public int compare(final SearchNode o1, final SearchNode o2) {
				if(!Objects.equal(o1.realFilePath.getParent(), o2.realFilePath.getParent())){
					throw new IllegalStateException();
				}
				return o1.nodeName.compareTo(o2.nodeName);
			}});//有序
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
