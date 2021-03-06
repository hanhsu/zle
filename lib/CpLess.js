const srcDir = process.argv[2];
const destDir = process.argv[3];
const zulPath = process.argv[4];
const zkmaxPath = process.argv[5];
const theme = process.argv[6] ? process.argv[6] : '';
const genDir = 'tempgen/archive/';

(function() {
	var fs = require('fs'),
		walk = require('walk'),
		dir = require('node-dir'),
		admzip = require('adm-zip'),
		less = require('less'),
		Str = require('string'),
		rmrf = require('rimraf'),
		mkpath = require('./mkpath'),
		helper = require('./SyntaxHelper'),
		files = [],
		imports = [],
		targets = [],
		current = '',
		imcFlag = zkmaxPath ? 2 : 1; //use to check if copy of imports is done or not

	//starting point
	walkOn();

	process.on('uncaughtException', function(err) {
		removeGenDir(err);
		console.error('Caught exception: ' + (err.stack ? err.stack : err) + '\n');
	});

	function walkOn() {
		var walker = walk.walk(srcDir, {followLinks: false});
		//walk through the root directory and find every .less files and put them into imports and targets
		walker.on('file', function(root, stat, next) {
			var fn = stat.name;
			if (Str(fn).endsWith('.less') && root.indexOf('_') < 0) { // ignore _folders
				if (Str(fn).startsWith('_')) {
					imports.push(root + '/' + fn);
				} else {
					targets.push(root + '/' + fn);
				}
			}
			setImmediate(next);
		});

		//encode the founded imports and save them in genDir folder
		walker.on('end', function() {
			if (imports.length > 0 || targets.length > 0) {
				copyImports();
			}
		});
	}

	function copyImports() {
		if (zulPath) {
			if (!fs.existsSync(zulPath)) {
				removeGenDir();
				throw new Error('incorrect path for zul');
			} else {
				copyImportsFiles(zulPath);	
			}
		}

		if (zkmaxPath) {
			if (!fs.existsSync(zkmaxPath)) {
				removeGenDir();
				throw new Error('incorrect path for zkmax');
			} else {
				copyImportsFiles(zkmaxPath);	
			}
		}
	}

	function copyImportsFiles(path) {
		if (fs.lstatSync(path).isDirectory()) {
			var dirWalker = walk.walk(path, {followLinks: false});
			dirWalker.on('file', function(root, stat, next) {
				var fn = stat.name;
				if (Str(fn).endsWith('.less') && Str(fn).startsWith('_')) {
					var fp = root + '/' + fn;
					var genpath = genDir + fp.substring(fp.indexOf('web'));
					mkpath.sync(genpath.replace(fn, ''), 0700);
					log('copying.....' + fp);
					data = fs.readFileSync(fp, 'utf-8');
					data = helper.encodeDsp(data, theme, genDir);
					fs.writeFileSync(genpath, data);
				} 
				setImmediate(next);
			});

			dirWalker.on('end', function() {
				importCheck();
			});

		} else if (fs.lstatSync(path).isFile()) {
			var zip = new admzip(path);
    		var zipEntries = zip.getEntries();
    		zipEntries.forEach(function(zipEntry) {
    			var fn = zipEntry.name;
    			var fp = zipEntry.entryName;

		        if (Str(fn).startsWith('_') && Str(fn).endsWith('.less')) {
		            var data = zip.readAsText(fp);
		            var genpath = genDir + fp;
					mkpath.sync(genpath.replace(fn, ''), 0700);
					log('copying.....' + fp);
					data = helper.encodeDsp(data, theme, genDir);
					fs.writeFileSync(genpath, data);
		        }
		    });
		   	importCheck();
		}
	}

	function importCheck() {
		if ( --imcFlag == 0) {
			encodeImports();
		}
	}

	function encodeImports() {
		if (imports.length > 0)
			var last = imports[imports.length - 1];

		for (var i = 0; i < imports.length; i++) {
			var cf = imports[i],
				data = fs.readFileSync(cf, 'utf-8');
			if (data) {
				data = helper.encodeDsp(data, theme, genDir);
				if (Str(cf).contains('web')) {
					var tempPath = genDir + cf.substring(cf.indexOf('web'));
				}
				mkpath.sync(tempPath.substring(0, tempPath.lastIndexOf('/')), 0700);
				fs.writeFileSync(tempPath, data);
			}
		}
		// compile the targets
		lessCompile();
	}

	function lessCompile() {
		var count = targets.length;
		for (var i = 0; i < targets.length; i++) {
			var cp = targets[i];
			console.log('compiling: ' + cp);
			compile(cp, function(css, path) {
				//replace path from src to dest
				var newPath = path.replace(srcDir, destDir).replace('\\less', '\\css').replace('/less', '/css').replace('.less', '.css.dsp.src');
				if(theme) {
					newPath = newPath.replace('/web', '/web/' + theme);
				}
				mkpath.sync(newPath.substring(0, newPath.lastIndexOf('/')), 0700);
				fs.writeFileSync(newPath, css);
				if (--count == 0) {
					removeGenDir();
				}
			});
		}
	}

	function compile(path, callback) {
		fs.readFile(path, 'utf-8', function (err, data) {
			if (err) {
				removeGenDir();
				throw err;
			}

			var data = helper.encodeDsp(data, theme, genDir),
		  	    parser = new(less.Parser);
		        
	  		parser.parse(data, function(err, tree) {
				if (err) {
					removeGenDir();
					throw err;					
				}
				var css = tree.toCSS();
				css = helper.decodeDsp(css);
				if (callback) {
					callback(css, path);
				}
			});
		});
	}

	function removeGenDir(error) {
		rmrf(genDir.substring(0, genDir.indexOf('archive')), function (err) {
			if (err) {
				throw err;
			}
			if (error)
				process.exit(1);
		});
	}

	function log(str) {
		console.log(str);
	}

})();