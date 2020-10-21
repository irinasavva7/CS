package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewController implements Initializable {
    private static final Logger LOG = Logger.getLogger(ViewController.class.getName());
    @FXML
    private static String finalJSON = "";
    @FXML
    private TextField urlTextField;
    @FXML
    private Button ConvertAndSave;
    private Future<List<String>> future;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TextFileReader reader = new TextFileReader();

    public void initialize(URL url, ResourceBundle rb) {
    }

    public void saveFile(ActionEvent event) throws IOException {
        event.consume();
        JSONArray jsonArr = new JSONArray(finalJSON);
        String pattern = "yyMMddHHmmssZ";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String filename = "jsonFile" + simpleDateFormat.format(new Date()) + ".json";
        FileWriter file = new FileWriter(filename);
        file.write(jsonArr.toString());
        file.close();
    }

    @FXML
    public void readAndConvertFile() throws InterruptedException, ExecutionException {

        future = executorService.submit(new Callable<List<String>>() {
            public List<String> call() throws Exception {
                return Collections.singletonList(reader.read(new File(ViewController.this.urlTextField.getText())));
            }
        });


        String text = future.get().get(0);
        Pattern pattern = Pattern.compile("<custom_item>(.*?)</custom_item>", Pattern.DOTALL);
        Matcher m = pattern.matcher(text.replaceAll(" +", " ").trim());
        executorService.shutdownNow();
        List<String> array = new ArrayList<>();
        List<String> finalArray = new ArrayList<>();
        while (m.find()) {
            String matchText = "";
            for (String sentence : m.group(1).split("\\r\\n")) {
                if (sentence.trim().length() > 0) {
                    if (sentence.contains("\"")) {
                        sentence = sentence.replaceAll("\"", "");
                    }
                    sentence = sentence.trim().replaceAll("\\b\\s(:)\\s\\b", "\":\"");
                    matchText += "\"" + sentence + "\"" + ",\n";
                    array.add(matchText);
                }
            }


            String singleItem = array.get(array.size() - 1);
            //delete the last ,
            finalArray.add("{\n" + singleItem.replaceFirst(",(?=[^,]+$)", "") + "}");
        }

        for (String s : finalArray) {
            finalJSON = finalJSON.length() > 0 ? finalJSON + ",\n" + s : s;
        }

        System.out.println(finalJSON);
        finalJSON = "[" + finalJSON.replace("\n", "").replace("\\", "|") + "]";
    }
}