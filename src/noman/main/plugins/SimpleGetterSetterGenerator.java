package noman.main.plugins;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import noman.main.annotations.Setter;

import java.util.stream.Collectors;

public class SimpleGetterSetterGenerator {
	
	private final TreeMaker treeMaker;
	private final Context context;
	private final Names names;
	
	public SimpleGetterSetterGenerator(final JavacTask javacTask) {
		this.context = ((BasicJavacTask) javacTask).getContext();
		this.treeMaker = TreeMaker.instance(context);
		this.names = Names.instance(context);
	}
	
	private JCTree.JCMethodDecl createMethodDefinition(long modifiers, Name methodName, JCTree.JCExpression returnType, JCTree.JCBlock body) {
		return treeMaker.MethodDef(
				treeMaker.Modifiers(modifiers),
				methodName,
				returnType,
				List.nil(), List.nil(), List.nil(),
				body,
				null
		);
	}
	
	
	public void generateFor(CompilationUnitTree compilationUnitTree) {
		compilationUnitTree.getTypeDecls().stream()
				.filter(tree -> tree instanceof JCTree.JCClassDecl)
				.forEach(jcClassDecl -> {
					List<JCTree> definitions = ((JCTree.JCClassDecl) jcClassDecl).defs;
					((JCTree.JCClassDecl) jcClassDecl).defs = ((JCTree.JCClassDecl) jcClassDecl).defs.appendList(createGettersFor(definitions));
				});
	}
	
	private List<JCTree> createGettersFor(List<JCTree> definitions) {
		return List.from(definitions.stream()
				.filter(tree -> (tree instanceof JCTree.JCVariableDecl) &&
						((JCTree.JCVariableDecl) tree).getModifiers().getAnnotations().stream()
								.anyMatch(annotation -> Setter.class.getSimpleName()
										.equals(annotation.getAnnotationType().toString())))
				.map(jcVarDecl -> createSetter(((JCTree.JCVariableDecl) jcVarDecl)))
				.collect(Collectors.toList()));
	}

//	private JCTree.JCMethodDecl createGetter(JCTree.JCVariableDecl jcVarDecl) {
//		String name = jcVarDecl.getName().toString();
//		Name methodName = names.fromString("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
//
//		JCTree.JCIdent varIdent = treeMaker.Ident(jcVarDecl.getName());
//		JCTree.JCReturn returnStmt = treeMaker.Return(varIdent);
//
//		JCTree.JCBlock methodBody = treeMaker.Block(0, List.of(returnStmt));
//
//		return createMethodDefinition(Flags.PUBLIC, methodName, jcVarDecl.vartype, methodBody);
//	}
	
	private JCTree.JCMethodDecl createSetter(JCTree.JCVariableDecl jcVarDecl) {
		String name = jcVarDecl.getName().toString();
		JCTree.JCIdent varIdent = treeMaker.Ident(jcVarDecl.getName());
		
		String camelCasedParamName = "p" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		Name methodName = names.fromString("set" + camelCasedParamName.substring(1));
		
		JCTree.JCVariableDecl paramVarDecl = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER),
				names.fromString(camelCasedParamName), jcVarDecl.vartype, null);
		JCTree.JCIdent paramIdent = treeMaker.Ident(paramVarDecl.getName());
		
//		JCTree.JCStatement assign = treeMaker.Assignment(varIdent.sym, paramIdent);
//		JCTree.JCBlock methodBody = treeMaker.Block(0, List.of(assign));
//
		JCTree.JCBlock methodBody = treeMaker.Block(0, List.nil());
		
		return treeMaker.MethodDef(
				treeMaker.Modifiers(Flags.PUBLIC),
				methodName,
				treeMaker.TypeIdent(TypeTag.VOID),
				List.nil(),
				List.of(paramVarDecl),
				List.nil(),
				methodBody,
				null
		);
	}
	
}
