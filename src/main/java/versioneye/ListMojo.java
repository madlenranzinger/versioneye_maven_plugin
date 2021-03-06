package versioneye;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo( name = "list", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ListMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;

    @Component
    private RepositorySystem system;

    @Parameter( defaultValue="${project}" )
    private MavenProject project;

    @Parameter( defaultValue="${repositorySystemSession}" )
    private RepositorySystemSession session;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> repos;

    public void execute() throws MojoExecutionException {
        versionEyeOutput();
        try{
            DependencyUtils dependencyUtils = new DependencyUtils();
            CollectRequest collectRequest = dependencyUtils.getCollectRequest(project, repos);
            DependencyNode root = system.collectDependencies(session, collectRequest).getRoot();
            DependencyRequest dependencyRequest = new DependencyRequest(root, null);

            system.resolveDependencies(session, dependencyRequest);

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            root.accept(nlg);

            List<Artifact> dependencies          = dependencyUtils.collectAllDependencies(nlg.getDependencies(true));
            List<Artifact> directDependencies    = dependencyUtils.collectDirectDependencies(root.getChildren());
            List<Artifact> recursiveDependencies = new ArrayList<Artifact>(dependencies);
            recursiveDependencies.removeAll(directDependencies);

            produceNiceOutput(dependencies, directDependencies, recursiveDependencies);
        } catch( Exception exception ){
            exception.printStackTrace();
            getLog().error("Oh no! Something went wrong. Get in touch with the VersionEye guys and give them feedback.");
            getLog().error( exception );
        }
    }

    private void produceNiceOutput(List<Artifact> dependencies, List<Artifact> directDependencies, List<Artifact> recursiveDependencies){
        productNiceOutputForDirectDependencies(directDependencies);
        productNiceOutputForRecursiveDependencies(recursiveDependencies);
        produceNiceOutputSummary(directDependencies.size(), recursiveDependencies.size(), dependencies.size());
    }

    private void versionEyeOutput(){
        getLog().info("");
        getLog().info("************* \\_/ VersionEye \\_/ *************");
        getLog().info("");
    }

    private void productNiceOutputForDirectDependencies(List<Artifact> directDependencies){
        getLog().info("");
        getLog().info("Direct Dependencies: ");
        getLog().info("--------------------");
        for (Artifact artifact : directDependencies){
            getLog().info(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
        }
        getLog().info("");
    }

    private void productNiceOutputForRecursiveDependencies(List<Artifact> recursiveDependencies){
        getLog().info("");
        getLog().info("Recursive Dependencies: ");
        getLog().info("--------------------");
        for (Artifact artifact : recursiveDependencies){
            getLog().info(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
        }
        getLog().info("");
    }

    private void produceNiceOutputSummary(int directCount, int recursiveCount, int allCount){
        getLog().info("");
        getLog().info(directCount + " Direct dependencies and " +
                recursiveCount + " recursive dependencies. This project has " +
                allCount + " dependencies.");
        getLog().info("");
        getLog().info("");
    }

}
