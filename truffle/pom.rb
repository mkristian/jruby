version = File.read( File.join( basedir, '..', 'VERSION' ) ).strip
project 'JRuby Truffle' do

  model_version '4.0.0'
  inherit 'org.jruby:jruby-parent', version
  id 'org.jruby:jruby-truffle'

  properties( 'tesla.not.dump.pom' => 'pom.xml',
              'tesla.not.dump.readonly' => true,

              'jruby.basedir' => '${basedir}/..' )

  jar 'org.jruby:jruby-core', '${project.version}', :scope => 'provided'

  jar 'com.oracle:truffle:0.6'
  jar 'com.oracle:truffle-dsl-processor:0.6', :scope => 'provided'

  plugin( :compiler,
          'encoding' => 'utf-8',
          'debug' => 'true',
          'verbose' => 'true',
          'fork' => 'true',
          'showWarnings' => 'true',
          'showDeprecation' => 'true',
          'source' => [ '${base.java.version}', '1.7' ],
          'target' => [ '${base.javac.version}', '1.7' ],
          'useIncrementalCompilation' =>  'false' ) do
    execute_goals( 'compile',
                   :id => 'default-compile',
                   :phase => 'compile',
                   'annotationProcessors' => [ 'com.oracle.truffle.dsl.processor.TruffleProcessor' ],
                   'generatedSourcesDirectory' =>  'target/generated-sources',
                   'compilerArgs' => [ '-XDignore.symbol.file=true',
                                       '-J-Duser.language=en',
                                       '-J-Dfile.encoding=UTF-8' ] )
  end

  plugin :shade do
    execute_goals( 'shade',
                   :id => 'create lib/jruby-truffle.jar',
                   :phase => 'package',
                   'outputFile' => '${jruby.basedir}/lib/jruby-truffle.jar' )
  end

  build do
    default_goal 'package'

    resource do
      directory 'src/main/ruby'
      includes '**/*rb'
    end
  end

  [ :dist, :'jruby-jars', :all, :release ].each do |name|
    profile name do
      plugin :shade do
        execute_goals( 'shade',
                       :id => 'pack jruby-truffle-complete.jar',
                       :phase => 'verify',
                       :artifactSet => { :includes => [ 'com.oracle:truffle' ] },
                       :shadedArtifactAttached =>  'true',
                       :shadedClassifierName =>  'complete' )
      end
    end
  end
end
