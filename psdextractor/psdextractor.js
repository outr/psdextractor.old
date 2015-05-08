var PSD = require('psd');
var fs = require('fs');
var mkdirp = require('mkdirp');
var path = require('path');
var rimraf = require('rimraf');
var util = require('util');

var file = process.argv[2];
var outputDirectory = process.argv[3];

console.log('Loading ' + file + '...');
PSD.open(file).then(function (psd) {
    try {
        var dir = path.join(outputDirectory);

        // Delete the directory
        if (fs.existsSync(dir)) {
            console.log('Deleting directory: ' + dir);
            rimraf.sync(dir);
        }

        // Make the directory
        console.log('Creating directory: ' + dir);
        mkdirp(dir);

        var tree = psd.tree();

        // Write the tree to file
        console.log('Writing tree to file.');
        fs.writeFile(path.join(dir, '/tree.json'), util.inspect(tree.export(), {depth: null}));

        // Write preview to file
        console.log('Writing preview to file.');
        psd.image.saveAsPng(path.join(dir, 'preview.png'));

        // Write the output file
        console.log('Opening output.json for writing...');
        var output = fs.createWriteStream(dir + '/output.json');
        output.write('{\n');
        output.write('  "width": ' + tree.get('width') + ',\n');
        output.write('  "height": ' + tree.get('height') + ',\n');
        output.write('  "layers": [\n');

        // Write out the images
        console.log('Iterating over descendants...');
        var descendants = tree.descendants();
        for (var i = 0; i < descendants.length; i++) {
            var layer = descendants[i];
            var exported = layer.export();
            if (exported.type == 'layer' && exported.visible == true) {
                output.write('    ');
                if (exported.text != null) {
                    exported.type = 'text';
                } else {
                    exported.type = 'image';
                }
                exported.index = i;
                output.write(JSON.stringify(exported));
                if (i < descendants.length - 1) {
                    output.write(',');
                }
                output.write('\n');
                if (exported.text == null) {
                    layer.saveAsPng(dir + '/' + layer.get('name') + '_' + i + '.png');
                }
            }
        }

        output.write('  ]\n');
        output.write('}\n');
        output.end();
        console.log('Finished extracting PSD.');
    } catch(err) {
        console.log('An error occurred!');
        console.log(err.stack);
    }
});
