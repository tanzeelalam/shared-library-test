use Data::Dumper;
use File::Basename;
use Time::localtime;

=pod

=head1 DESCRIPTION

This script takes in the name of manifest to execute and passes on the env variables needed to extract the
required data from the manifest file. Once the data is extracted, it is stored in a file which is then read back by the pipeline code,
processed and returned back to the Orbit user. It uses perls do keyword to open, pass the params and execute the script.

=cut

my ($manifestFile) = shift(@ARGV);

@params = @ARGV;

printf("[%s] deliver:  $manifestFile \@params:\n" . Dumper(@params) . "\n", ctime());
ProcessManifestFile("$manifestFile", @params) ||  die "Unexpected:  ProcessManifestFile Failed.\n";

sub ProcessManifestFile {
    my($manifestFileName) = shift();
    my(@params) = @_;
    my($manifestFileName_dump) = "${manifestFileName}_output.txt";

    printf("\n[%s] ProcessManifestFile ($manifestFileName):  Starting...: \n\@params = \n\t@params\n\n", ctime());

    if (not -f $manifestFileName) { printf("ProcessManifestFile ($manifestFileName): Unexpected:  Manifest file not found: $manifestFileName\n") && return(0); }

    if (@params) {
        my( @retval ) = do "$manifestFileName";
        # validate that the array is populated
        if ( $#retval < 0 ) {
            printf("\n[%s] ProcessManifestFile ($manifestFileName):  ERROR manifest returned empty list: \@retval = \"$#retval\"\n", ctime());
            die "Unexpected:  \@retval array is empty = \"$#retval\"\n";
            return(0);
        }
=pod

=head1 DESCRIPTION

The following code opens manifest "${manifestFileName}_output.txt" file and and dumps the required data into
it. This data was received from executing the (do "$manifestFileName";) code.

=cut
        open(FILEOUT, ">$manifestFileName_dump");
        print "Example:\n";
        foreach $filepath ( @retval ) {
            if ( ( (@$filepath[0]) ) && ( (@$filepath[1]) ) && ( (@$filepath[2]) ) ) {
                print(FILEOUT "@${filepath[0]},@${filepath[1]},@${filepath[2]}\n");
            }
            elsif ( ( (@$filepath[0]) ) && ( (@$filepath[1]) ) ) {
                print(FILEOUT "@${filepath[0]},@${filepath[1]}\n"); # dump processed data to file
            }
            else {
                print(FILEOUT "$filepath\n"); # dump processed data to file
            }
        }
        close(FILEOUT);

    } else {
        printf("ProcessManifestFile ($manifestFileName): Unexpected:  Manifest \@params undefined.\n") && return(0);
    }

    printf("[%s] ProcessManifestFile ($manifestFileName):  Completed.\n", ctime());
    return(1);
}
