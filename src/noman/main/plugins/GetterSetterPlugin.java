package noman.main.plugins;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;

public class GetterSetterPlugin implements Plugin {
	
	@Override
	public String getName() {
		return "Getter";
	}
	
	@Override
	public void init(JavacTask javacTask, String... strings) {
		javacTask.setTaskListener(new TaskListener() {
			@Override
			public void started(TaskEvent taskEvent) {
				// do nothing
			}
			
			@Override
			public void finished(TaskEvent taskEvent) {
				if (TaskEvent.Kind.PARSE.equals(taskEvent.getKind())) {
					System.out.println("Before: ");
					System.out.println(taskEvent.getCompilationUnit());
					
					new SimpleGetterSetterGenerator(javacTask).generateFor(taskEvent.getCompilationUnit());
					
					System.out.println("\nAfter:");
					System.out.println(taskEvent.getCompilationUnit());
				}
			}
		});
	}
}
