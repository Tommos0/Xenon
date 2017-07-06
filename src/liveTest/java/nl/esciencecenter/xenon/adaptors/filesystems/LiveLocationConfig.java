package nl.esciencecenter.xenon.adaptors.filesystems;

import java.util.AbstractMap;
import java.util.Map;

import nl.esciencecenter.xenon.filesystems.Path;

public class LiveLocationConfig extends LocationConfig{
    @Override
    public Path getExistingPath() {
        return new Path("/home/xenon/filesystem-test-fixture/links/file0");
    }

    @Override
    public Map.Entry<Path, Path> getSymbolicLinksToExistingFile() {
        return new AbstractMap.SimpleEntry<>(
            new Path("/home/xenon/filesystem-test-fixture/links/link0"),
            new Path("/home/xenon/filesystem-test-fixture/links/file0")
        );
    }
}
