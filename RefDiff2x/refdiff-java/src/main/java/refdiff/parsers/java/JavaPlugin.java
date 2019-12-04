package refdiff.parsers.java;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import refdiff.core.io.FilePathFilter;
import refdiff.core.io.GitSourceTree;
import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Stereotype;
import refdiff.parsers.LanguagePlugin;

import static java.util.stream.Collectors.toMap;
import static refdiff.parsers.java.SDModelBuilder.systemFileSeparator;

public class JavaPlugin implements LanguagePlugin {

	private File tempDir = null;
	private final JavaSourceTokenizer tokenizer = new JavaSourceTokenizer();
	
	public JavaPlugin() {}
	
	public JavaPlugin(File tempDir) {
		this.tempDir = tempDir;
	}

	@Override
	public CstRoot parse(SourceFileSet sources) throws Exception {
		List<String> javaFiles = new ArrayList<>();
		Optional<Path> optBasePath = sources.getBasePath();
		if (!optBasePath.isPresent()) {
			if (this.tempDir == null) {
				throw new RuntimeException("The JavaParser requires a SourceFileSet that is materialized on the file system. Either pass a tempDir to JavaParser's contructor or call SourceFileSet::materializeAt before calling this method.");
			} else {
				sources.materializeAtBase(tempDir.toPath());
				optBasePath = sources.getBasePath();
			}
		}
		if(sources instanceof GitSourceTree){
			GitSourceTree gst = (GitSourceTree) sources;
			Map<String, String> map = new HashMap<>();
			for (SourceFile sourceFile : sources.getSourceFiles()) {
				String content = gst.readContent(sourceFile);
				map.put(sourceFile.getPath(), content);
				javaFiles.add(sourceFile.getPath());
			}

			File rootFolder = optBasePath.get().toFile();

			SDModel sdModel = new SDModel();
			SDModelBuilder mb = new SDModelBuilder();
			mb.analyze(rootFolder, map.entrySet().stream().collect(toMap(x ->  x.getKey().replaceAll("/", systemFileSeparator), Map.Entry::getValue))
					, sdModel, tokenizer);

			return sdModel.getRoot();

		}else {
			for (SourceFile sourceFile : sources.getSourceFiles()) {
				javaFiles.add(sourceFile.getPath());
			}
			File rootFolder = optBasePath.get().toFile();

			SDModel sdModel = new SDModel();
			SDModelBuilder mb = new SDModelBuilder();
			mb.analyze(rootFolder, javaFiles, sdModel, tokenizer);

			return sdModel.getRoot();
		}
		

	}

	public static String getKey(CstNode node) {
		String parentName;
		if (node.getParent().isPresent()) {
			parentName = getKey(node.getParent().get()) + ".";
		} else if (node.getNamespace() != null) {
			parentName = node.getNamespace();
		} else {
			parentName = "";
		}
		if (node.getType().equals(NodeTypes.METHOD_DECLARATION)) {
			String key = parentName + AstUtils.normalizeMethodSignature(node.getLocalName());
			
			// convert org.MyClass.new() to org.MyClass.MyClass()
			if (node.hasStereotype(Stereotype.TYPE_CONSTRUCTOR)) {
				key = key.replace(".new(", "." + node.getParent().get().getSimpleName() + "(");
			}
			return key;
		}
		
		return parentName + node.getLocalName();
	}
	
	@Override
	public FilePathFilter getAllowedFilesFilter() {
		return new FilePathFilter(Arrays.asList(".java"));
	}
}
