package org.refactoringminer;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllBytes;

public class ProtoUtil {


    /**
     *
     * @param kind Folder for the "Type" of proto
     * @return the folder
     */
    public static Function<Path,Path> folderName(String kind){
        switch (kind){
//            case "RMined" : return p -> p.resolve("RMined");
            default: return p -> p;
        }
    }

    public static <T> CheckedFunction1<CodedInputStream,T> parser(String kind){
        switch (kind){
            case "MatchedStatements" : return p -> (T) MatchedStatementsOuterClass.MatchedStatements.parseFrom(p);
            default: return c -> null;
        }
    }



    public static class ReadWriteAt {

        private final Path outputDir;

        /**
         *
         * @param outputD Output directory where protos should be generated
         */
        public ReadWriteAt(Path outputD){
            this.outputDir = outputD;
        }

        /**
         *
         * @param msg The protobuf message
         * @param fileName Name of the file for the message
         * @param append should append or separate file
         */
        public void write(GeneratedMessageV3 msg, String fileName, boolean append) {
            Path folderName = folderName(msg.getDescriptorForType().getName()).apply(outputDir);
            if (append) {
                appendToFile(createIfAbsent(folderName.resolve(fileName + ".txt")), msg::writeTo);
                appendToFile(createIfAbsent(folderName.resolve(fileName + "BinSize.txt")), o -> o.write((msg.getSerializedSize() + " ").getBytes(Charset.forName("UTF-8"))) );
            }else{
                writeToFile(createIfAbsent(folderName.resolve(fileName + ".txt")), msg::writeTo);
            }

        }

        public static void appendToFile(Path p, CheckedConsumer<FileOutputStream> content){
            Try.of(() -> {
                FileOutputStream output = new FileOutputStream(p.toString(), true);
                content.accept(output);
                output.close();
                return true;
            }).getOrElseThrow(() -> new RuntimeException("Could not append to file"));

        }

        public static Path createIfAbsent(Path p){
            return Try.of(() -> p.toFile().exists() ? p : Files.createFile(p))
                    .getOrElseThrow(() -> new RuntimeException("Could Not create file " + p.toString()));
        }

        public static void writeToFile(Path p, CheckedConsumer<FileOutputStream> content){
            Try.of(() -> {
                FileOutputStream output = new FileOutputStream(p.toString(), false);
                content.accept(output);
                output.close();
                return true;
            }).getOrElseThrow(() -> new RuntimeException("Could not append to file"));

        }


        /**
         *
         * @param fileName Name of the file to read
         * @param kind "Type" of protobuff msg
         * @param <T> protobuf message
         * @return List of messages in the file
         */
        public <T> List<T> readAll(String fileName, String kind){
            Path folderName = folderName(kind).apply(outputDir);

            List<T> msgs = new ArrayList<>();

            if(!Files.exists(folderName.resolve(fileName + ".txt")))
                return msgs;

            // Try to get all binary message sizes
            Try<List<Integer>> msgSizes = Try.of(() -> new String(readAllBytes(createIfAbsent(folderName.resolve(fileName + "BinSize.txt")))))
                    .map(x -> x.split(" "))
                    .map(x -> Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList()))
                    .onFailure(e -> System.out.println("Could not read the sizes of the messages for " + fileName + "   " + e.toString()));

            // Try to read binary
            Try<FileInputStream> contentStream = Try.of(() -> new FileInputStream(createIfAbsent(folderName.resolve(fileName + ".txt")).toString()))
                    .onFailure(e -> System.out.println("Could not read the messages for " + fileName + "   " + e.toString()));

            if(msgSizes.isSuccess() && contentStream.isSuccess()){
                FileInputStream content = contentStream.get();

                int notReadCounter = msgSizes.get().stream()
                        .mapToInt(c -> {
                            byte[] b = new byte[c];
                            return Try.of(() -> content.read(b))
                                    .filter(i -> i > 0)
                                    .flatMap(i -> Try.of(() -> ProtoUtil.<T>parser(kind).apply(CodedInputStream.newInstance(b)))
                                            .onSuccess(msgs::add)
                                            .onFailure(e -> System.out.println("Could not read message for " + fileName + "   " + e.toString()))
                                            // if success nothing
                                            .map(a -> 0))
                                    // if failure add 1 to notReadCounter
                                    .getOrElse(1);
                        }).sum();

                if(notReadCounter>0)
                    System.out.println("Could not read " + notReadCounter + " messages in the file " + fileName);

                return msgs;

            }else{
                System.out.println("Could not read binary or binary size");
                return msgs;
            }
        }
        public <T> Try<T> read(String fileName, String kind){
            return Try.of(() -> ProtoUtil.<T>parser(kind)
                    .apply(CodedInputStream.newInstance(readAllBytes(outputDir.resolve(fileName + ".txt")))))
                    .onFailure(e -> System.out.println(e.toString()));
        }
    }
}
