# themes-plugin

This plugin supports adding Theme packs to sites. 

To add the plugin to the project, add the following to your top level pom.xml.

    <dependency>
      <groupId>com.atex.gong.plugins</groupId>
      <artifactId>themes-plugin</artifactId>
      <version>1.0</version>
    </dependency>
    
    <dependency>
      <groupId>com.atex.gong.plugins</groupId>
      <artifactId>themes-plugin</artifactId>
      <version>1.0</version>
      <classifier>contentdata</classifier>
    </dependency>
    

Add a Tab to your page layout to support setting the theme:

  <layout name="themePage" input-template="p.Page" label="Theme Options">
				<param name="lazyInit">true</param>
				<layout name="resourcelibraries" input-template="p.Column">
					<layout name="themeSlotComment" input-template="p.Comment" label="Theme Slot">
						<param name="comment">This will hold the Theme Element</param>
					</layout>
					<field name="themeSlot" input-template="p.siteengine.layout.Slot.it">
						<param name="defaultInheritSetting">true</param>
						<param name="displayInheritSetting">false</param>
					</field>
				</layout>
  </layout>

Add a theme .content for to your project, add put the files needed as relative to the .content file. e.g. in the example below, there will be a folder called assets, with subfolders of css, img & js.

    ### THEME ###
    
    id:my.ThemeElement.e
    major:layoutelement
    inputtemplate:com.atex.plugins.themes.ThemeElement
    securityparent:GreenfieldOnline.d
    name:My Theme
    component:cssdir:count:1
    component:cssdir[0]/cssfile:fileName:index.css
    component:cssdir[0]/cssfile:path:root/css/cssdir[0]/cssfile
    component:isBase:value:true
    component:jsfootdir:count:2
    component:jsfootdir[0]/jsfootfile:fileName:index.js
    component:jsfootdir[0]/jsfootfile:path:root/js/jsfootdir[0]/jsfootfile
    component:jsfootdir[1]/jsfootfile:fileName:jquery-ui.js
    component:jsfootdir[1]/jsfootfile:path:root/js/jsfootdir[1]/jsfootfile
    file:root/css/cssdir[0]/cssfile/index.css:assets/css/index.css
    file:root/js/jsfootdir[0]/jsfootfile/index.js:assets/js/build/index.js
    file:root/js/jsfootdir[1]/jsfootfile/jquery-ui.js:assets/js/lib/vendor/jquery-ui.js
    
    #### CSS
    file:file/index.css:assets/css/index.css
    file:file/index-ie8.css:assets/css/index-ie8.css
    file:file/index-sg.css:assets/css/index-sg.css
    
    #### JavaScript
    file:file/index.js:assets/js/build/index.js
    file:file/modernizr.js:assets/js/build/modernizr.js
    file:file/html5shiv.js:assets/js/lib/vendor/html5shiv.js
    file:file/jquery-ui.js:assets/js/lib/vendor/jquery-ui.js
    
    
    #### Images
    file:file/arrow--select.svg:assets/img/content/arrow--select.svg
    file:file/header-image.png:assets/img/content/header-image.png
    file:file/logo.jpg:assets/img/content/logo.jpg
    file:file/logo.png:assets/img/content/logo.png
    file:file/logo.svg:assets/img/content/logo.svg
    file:file/logo-text.svg:assets/img/content/logo-text.svg
    file:file/logo-text.png:assets/img/content/logo-text.png
    file:file/logo-text.jpg:assets/img/content/logo-text.jpg
    file:file/tick--blue.svg:assets/img/content/tick--blue.svg
    file:file/apple-touch-icon-precomposed-76x76.png:assets/img/meta/apple-touch-icon-precomposed-76x76.png
    file:file/apple-touch-icon-precomposed-120x120.png:assets/img/meta/apple-touch-icon-precomposed-120x120.png
    file:file/apple-touch-icon-precomposed-152x152.png:assets/img/meta/apple-touch-icon-precomposed-152x152.png
    file:file/apple-touch-icon-precomposed-180x180.png:assets/img/meta/apple-touch-icon-precomposed-180x180.png
    file:file/favicon.ico:assets/img/meta/favicon.ico
    file:file/favicon-16.png:assets/img/meta/favicon-16.png
    file:file/favicon-32.png:assets/img/meta/favicon-32.png
    file:file/meta-icon-1000x1000.jpg:assets/img/meta/meta-icon-1000x1000.jpg
    file:file/microsoft-pinned-tile-image-144x144.png:assets/img/meta/microsoft-pinned-tile-image-144x144.png
    file:file/icon-sprite.png:assets/img/sprite/generated/icon-sprite.png
    file:file/icon-sprite.svg:assets/img/sprite/generated/icon-sprite.svg
    file:file/pen.jpg:assets/img/structure/pen.jpg
    file:file/transparent-bg-1x1.png:assets/img/structure/transparent-bg-1x1.png
    
    ### END THEME ###
			
			
