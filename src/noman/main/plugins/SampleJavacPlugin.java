package noman.main.plugins;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import noman.main.annotations.Positive;

import java.util.*;
import java.util.stream.Collectors;

public class SampleJavacPlugin implements Plugin {
	
	public static final Set<String> TARGET_TYPES = new HashSet<>(Arrays.asList(
			byte.class.getName(), short.class.getName(), char.class.getName(),
			int.class.getName(), long.class.getName(), float.class.getName(), double.class.getName()
	));
	
	@Override
	public String getName() {
		return "MyPlugin";
	}
	
	@Override
	public void init(JavacTask task, String... args) {
		
		Context context = ((BasicJavacTask) task).getContext();
		
		task.addTaskListener(new TaskListener() {
			@Override
			public void started(TaskEvent taskEvent) {
			
			}
			
			@Override
			public void finished(TaskEvent taskEvent) {
				// We want to analyze the abstract tree of the after the code is finished parsing
				if (taskEvent.getKind() != TaskEvent.Kind.PARSE) {
					return;
				}
				
				taskEvent.getCompilationUnit().accept(new TreeScanner<Void, Void>() {
					@Override
					public Void visitMethod(MethodTree method, Void unused) {
						List<VariableTree> parametersToInstrument = method.getParameters().stream().
								filter(SampleJavacPlugin.this::shouldInstrument).
								collect(Collectors.toList());
						if (!parametersToInstrument.isEmpty()) {
							Collections.reverse(parametersToInstrument);
							parametersToInstrument.forEach(parameter -> addCheck(method, parameter, context));
						}
						if (method.getReturnType() != null) {
							LC(context, "" + method.getReturnType().getClass().getName() + method.getReturnType().getClass());
						}
						return super.visitMethod(method, unused);
					}
					
				}, null);
			}
		});
		
	}
	
	
	private boolean shouldInstrument(VariableTree parameter) {
		return TARGET_TYPES.contains(parameter.getType().toString()) &&
				parameter.getModifiers().getAnnotations().stream().
						anyMatch(annotation -> Positive.class.getSimpleName().
								equals(annotation.getAnnotationType().toString()));
	}
	
	
	private void addCheck(MethodTree method, VariableTree parameter, Context context) {
		
		JCTree.JCIf check = createCheck(parameter, context);
		JCTree.JCBlock body = (JCTree.JCBlock) method.getBody();
		body.stats = body.stats.prepend(check);
//		LC(context, "After:\n");
//		LC(context, method.toString());
	}
	
	private static JCTree.JCIf createCheck(VariableTree parameter, Context context) {
		TreeMaker factory = TreeMaker.instance(context);
		Names symbolsTable = Names.instance(context);
		
		return factory.at(((JCTree) parameter).pos).If(factory.Parens(createIfCondition(factory, symbolsTable, parameter)),
				createIfBlock(factory, symbolsTable, parameter), null);
	}
	
	private static JCTree.JCBinary createIfCondition(TreeMaker factory, Names symbolsTable, VariableTree parameter) {
		Name parameterId = symbolsTable.fromString(parameter.getName().toString());
		return factory.Binary(JCTree.Tag.LE, factory.Ident(parameterId), factory.Literal(TypeTag.INT, 0));
	}
	
	private static JCTree.JCBlock createIfBlock(TreeMaker factory, Names symbolsTable, VariableTree parameter) {
		String parameterName = parameter.getName().toString();
		Name parameterId = symbolsTable.fromString(parameterName);

//		String errorMessagePrefix = String.format("Argument %s of type %s is marked by @%s but got '",
//				parameterName, parameter.getType(), Positive.class.getSimpleName());
		
		String errorMessagePrefix = "Crazy Bitch, only I got room for positives";
		
		String errorMessageSuffix = "' for it";
		
		return factory.Block(0, com.sun.tools.javac.util.List.of(
				factory.Throw(
						factory.NewClass(null, com.sun.tools.javac.util.List.nil(),
								factory.Ident(symbolsTable.fromString(IllegalAccessException.class.getSimpleName())),
								com.sun.tools.javac.util.List.of(
										factory.Binary(JCTree.Tag.PLUS,
												factory.Binary(JCTree.Tag.PLUS, factory.Literal(TypeTag.CLASS, errorMessagePrefix), factory.Ident(parameterId)),
												factory.Literal(TypeTag.CLASS, errorMessageSuffix))
								),
								null)
				)
		));
	}
	
	private void LC(Context context, String message) {
		Log.instance(context).printRawLines(message);
	}
	
}