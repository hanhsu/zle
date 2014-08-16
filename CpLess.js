
const srcDir = process.argv[2];
const destDir = process.argv[3];
const classpath = process.argv[4];
const theme = process.argv[5] ? process.argv[5] : '';

(function() {
	var walk = require('walk'),
		mkpath = require('./mkpath'),
		fs = require('fs'),	
		compiler = require('./LessEngine'),
		helper = require('./SyntaxHelper'),
		S = require('string'),
		files = [],
		imports = [],
		targets = [],
		current = '',
		walker = walk.walk(srcDir, {followLinks: false});

	walker.on('file', function(root, stat, next) {
		if (S(stat.name).endsWith('.less'))
			files.push(root + '/' + stat.name);
		next();
	});

	walker.on('end', function() {
		var fp, fn;

		for (var i = 0; i < files.length; i++) {

			fp = fn = files[i];
			fn = fn.substring(fn.lastIndexOf('/') + 1);

			if(S(fn).startsWith('_'))
				imports.push(fp);
			else {
				targets.push(fp);
			}
		}

		encodeImports();
	});

	function encodeImports() {
		if (imports.length > 0)
			var last = imports[imports.length - 1];

		for (var i = 0; i < imports.length; i++) {
			var cf = imports[i],
				data = fs.readFileSync(cf, 'utf-8');

			if (data) {
				data = helper.encodeDsp(data, '', classpath);
				var dst = cf.replace(srcDir, destDir);
				mkpath.sync(dst.substring(0, dst.lastIndexOf('/')), 0700);
				fs.writeFileSync(dst, data);
			}
		}
		lessCompile();
	}

	function encodeFiles() {
		if (files.length > 0)
			var last = files[files.length - 1];

		for (var i = 0; i < files.length; i++) {
			var cf = files[i],
				data = fs.readFileSync(cf, 'utf-8');

			if (data) {
				data = helper.encodeDsp(data, '', classpath);
				var dst = cf.replace(srcDir, destDir);
				mkpath.sync(dst.substring(0, dst.lastIndexOf('/')), 0700);
				fs.writeFileSync(dst, data);
				console.log(dst);
			}
		}
		lessCompile();
	}

	function lessCompile() {
		var last = targets[targets.length -1].replace(destDir, srcDir);
		for (var i = 0; i < targets.length; i++) {
			var cp = targets[i].replace(destDir, srcDir);
			console.log('compiling: ' + cp);
			compiler.compile(cp, classpath, theme, function(css, path) {
				//replace folder name
				var newPath = path.replace(srcDir, destDir).replace('/less', '/css');
				// replace theme foder name
				if(theme) {
					newPath = newPath.replace('/web', '/web/' + theme);
				}
				//replace file sub-name for source file
				var dspSrcPath = newPath.replace('.less', '.css.dsp.src');
				mkpath.sync(dspSrcPath.substring(0, dspSrcPath.lastIndexOf('/')), 0700);
				fs.writeFile(dspSrcPath, css, function(err) {
					if (err)
						throw err;
				});
			});	
		}
	}
})();