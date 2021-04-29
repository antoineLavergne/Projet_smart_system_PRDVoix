package fr.polytech.larynxapp.controller.home;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.net.Uri;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.writer.WriterProcessor;

import fr.polytech.larynxapp.R;
import fr.polytech.larynxapp.model.Record;
import fr.polytech.larynxapp.controller.analysis.FeaturesCalculator;
import fr.polytech.larynxapp.controller.analysis.PitchProcessor;
import fr.polytech.larynxapp.controller.analysis.Yin;
import fr.polytech.larynxapp.model.audio.AudioData;
import fr.polytech.larynxapp.model.database.DBManager;


public class HomeFragment extends Fragment {

    /**
     * The status of the mic button, can have the 3 status listed in Status_mic
     */
    private Status_mic status_mic_button;

    /**
     *  The reset button, only appear after a full record
     */
    private Button button_restart;

    private Button button_file;

    /**
     *  The font of the mic button, hold the button
     */
    private ImageView button_mic;

    /**
     *  The icon of the mic button
     */
    private ImageView icon_mic;

    /**
     *  Show the advancement of the recording
     */
    private ProgressBar progressBar;

    /**
     * The default file name.
     */
    public static final String FILE_NAME = "New Record.wav";

    /**
     * The storage path of the records.
     */
    public static final String FILE_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "voiceRecords";

    /**
     * The permission request's code.
     */
    private static final int    MY_PERMISSIONS_REQUEST_NUMBER = 1;

    private File file;

    /**
     * The shimmer value.
     */
    private double shimmer;

    /**
     * The jitter value.
     */
    private double jitter;

    /**
     * The HNR value.
     */
    private double HNR;

    /**
     * The record's fundamental frequency.
     */
    private double f0;

    /**
     * The permissions list needed for the recording.
     */
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * The permissions that still need to be added.
     */
    private List<String> mPermissionList = new ArrayList<>();

    /**
     * Boolean that verify the permissions.
     */
    private boolean granted = false;

    /**
     * The data base manager.
     */
    private DBManager manager;

    /**
     * The path used to save the file.
     */
    private String finalPath = FILE_PATH + File.separator + FILE_NAME;

    /**
     * File name corresponding to record's name
     */
    private String fileName;

    /**
     * List of pitch captured during the record (frequencies used to make the mean frequency and get the mean period)
     */
    private List<Float> pitches = new ArrayList<>();

    /**
     * AudioDispatcher managing the pitch detection and the wav file writing
     */
    private AudioDispatcher dispatcher;

    /**
     * TarsosDSPAudioFormat created to work with AudioDispatcher (see TarsosDSP)
     */
    private TarsosDSPAudioFormat AUDIO_FORMAT;

    /**
     * the shimmer's setter
     * @param shimmer shimmer
     */
    public void setShimmer(double shimmer) {
        this.shimmer = shimmer;
    }

    /**
     * the jitter's setter
     * @param jitter jitter
     */
    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    /**
     * the shimmer's getter
     * @return shimmer's value
     */
    public double getShimmer() {
        return shimmer;
    }

    /**
     * the jitter's getter
     * @return jitter's value
     */
    public double getJitter() {
        return jitter;
    }

    /**
     * create the home's view
     * @param inflater inflater Used to load the xml layout file as View
     * @param container A container component
     * @param savedInstanceState Used to save activity
     * @return Return a home's view object
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        initPermissions();
        // create the components
        manager = new DBManager( this.getContext() );
        button_restart = root.findViewById(R.id.reset_button);
        button_mic = root.findViewById(R.id.mic_button);
        button_file = root.findViewById(R.id.button);
        icon_mic = root.findViewById(R.id.mic_icon);
        status_mic_button = Status_mic.DEFAULT;
        progressBar = root.findViewById(R.id.progressBar_mic);
        progressBar.getProgressDrawable().setColorFilter(
                Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
        AUDIO_FORMAT = new TarsosDSPAudioFormat(
                44100,
                16,
                1,
                true,
                ByteOrder.LITTLE_ENDIAN.equals(ByteOrder.nativeOrder()));

        updateView();
        return root;
    }

    /**
     * Updates the view depending of the status of the mic button
     */
    private void updateView() {
        switch (status_mic_button) {
            case DEFAULT:
                progressBar.setVisibility(View.INVISIBLE);
                icon_mic.setBackgroundResource(R.drawable.ic_mic);
                button_mic.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        updateView(Status_mic.RECORDING);
                    }
                });
                button_file.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        startActivityForResult(intent, 1);
                    }

                });
                button_restart.setVisibility(View.GONE);
                break;

            case RECORDING:
                progressBar.setProgress(0);
                startRecording();
                progressBar.setVisibility(View.VISIBLE);
                icon_mic.setBackgroundResource(R.drawable.ic_stop_black_24dp);
                button_mic.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        updateView(Status_mic.CANCELED);
                    }
                });

                button_restart.setVisibility(View.GONE);
                break;

            case CANCELED:
                progressBar.setVisibility(View.VISIBLE);
                stopRecording();
                icon_mic.setBackgroundResource(R.drawable.ic_replay_black_24dp);
                button_mic.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        updateView(Status_mic.DEFAULT);
                    }
                });

                button_restart.setVisibility(View.GONE);
                break;

            case FINISH:
                progressBar.setVisibility(View.INVISIBLE);
                icon_mic.setBackgroundResource(R.drawable.ic_save_black_24dp);
                button_mic.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        save();
                        analyseData();
                        manager.updateRecordVoiceFeatures(fileName, jitter*100, shimmer, f0);

                        updateView(Status_mic.DEFAULT);
                    }
                });

                button_restart.setVisibility(View.VISIBLE);
                button_restart.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        updateView(Status_mic.DEFAULT);
                    }
                });
                break;

            case FILE_FINISH:
                save();
                analysePitchFromFile();
                manager.updateRecordVoiceFeatures(fileName, jitter, shimmer, f0);

                updateView(Status_mic.DEFAULT);
                break;
        }
    }

    /**
     * get the pitch's information from the stream of wave file
     */
    private void analysePitchFromFile(){
        AudioData audioData = new AudioData();
        boolean fileOK;
        file = new File(finalPath);

        try {
            if (!file.exists())
                //Ignored we don't need the result because we try to create only if the file doesn't exist.
                file.createNewFile();
            fileOK = true;
        }
        catch (IOException e) {
            Log.e("AnalyseData", e.getMessage(), e);
            fileOK = false;
        }

        if (!fileOK) {
            return;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);
            short[] s = new short[(b.length - 44) / 2];
            ByteBuffer.wrap(b)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(s);

            for (short ss : s) {
                audioData.addData(ss);
            }

            audioData.setMaxAmplitudeAbs();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        finally
        {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        audioData.processData();

        FeaturesCalculator featuresCalculator = new FeaturesCalculator(audioData);
        setPitches(featuresCalculator.calculatePitches());

        featuresCalculator.setContext(getContext());
        featuresCalculator.initPeriodsSearch();
        featuresCalculator.searchPitchPositions();
        this.shimmer = featuresCalculator.getShimmer();
        this.jitter = featuresCalculator.getJitter();
        f0 = featuresCalculator.getfundamentalFreq();

        System.out.println("from wav file");
        System.out.println("shimmer:"+shimmer);
        System.out.println("jitter"+jitter);
        System.out.println("f0:"+f0);

    }

    /**
     * Starts the recording
     */
    public void startRecording() {
        if ( granted ) {
            File folder = new File( FILE_PATH );
            if ( !folder.exists() ) {
                folder.mkdirs();
            }
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100, 2048, 0);

            File file = new File(FILE_PATH + File.separator + FILE_NAME);
            RandomAccessFile randomAccessFile = null;

            try {
                randomAccessFile = new RandomAccessFile(file,"rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            AudioProcessor recordProcessor = new WriterProcessor(AUDIO_FORMAT, randomAccessFile);
            dispatcher.addAudioProcessor(recordProcessor);

            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e){
                    float pitchInHz = res.getPitch();
                    if(pitchInHz != -1 && pitchInHz < 400){
                        pitches.add(pitchInHz);

                    }
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(new Yin(44100, 2048), 44100, 2048, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    long startTime = System.nanoTime();
                    long endTime = System.nanoTime();// start dispatcher
                    Thread thread = new Thread(dispatcher, "Audio Dispatcher");
                    thread.start();
                    while (endTime - startTime < 5000000000L && status_mic_button == Status_mic.RECORDING) {
                        progressBar.setProgress(Math.round((endTime - startTime) / 50000000f));
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        endTime = System.nanoTime();
                    }
                    if (endTime - startTime >= 5000000000L) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopRecording();
                                updateView(Status_mic.FINISH);
                            }
                        });
                    }
                }
            }).start();


        }
    }

    /**
     * Stops the recording
     */
    private void stopRecording()
    {
        releaseDispatcher();
    }

    /**
     * Releases the audio dispatcher processing the pitch and creating the wav file associated
     */
    private void releaseDispatcher()
    {
        if(dispatcher != null)
        {
            if(!dispatcher.isStopped())
                dispatcher.stop();
            dispatcher = null;
        }
    }

    /**
     * Changing the status of the mic button
     * @param newStatus the new status of the mic button
     */
    private void updateView(Status_mic newStatus) {
        setStatus_mic_button(newStatus);
        if(newStatus == Status_mic.RECORDING)
            getActivity().findViewById(R.id.nav_view).setActivated(false);
        updateView();
    }

    public Status_mic getStatus_mic_button() {
        return status_mic_button;
    }

    public void setStatus_mic_button(Status_mic status_mic_button) {
        this.status_mic_button = status_mic_button;
    }

    /**
     * OnPause method called when the phone goes in sleep mode
     */
    @Override
    public void onPause()
    {
        super.onPause();
    }

    /**
     * OnStop method called once the app is hidden (back to home or something like that)
     */
    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * OnDestroy method called after app termination
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        releaseDispatcher();
        manager.closeDB();
    }

    /**
     * OnDetach method called after leaving the app in background task
     */
    @Override
    public void onDetach()
    {
        super.onDetach();
        if(getStatus_mic_button() == Status_mic.RECORDING)
            stopRecording();
    }

    /**
     * Check the permissions of the application and request the permissions of the missing ones.
     */
    private void initPermissions() {
        this.mPermissionList.clear();
        for ( String permission : permissions ) {
            if ( ContextCompat.checkSelfPermission( getActivity(), permission ) != PackageManager.PERMISSION_GRANTED ) {
                mPermissionList.add( permission );
            }
        }

        if ( mPermissionList.isEmpty() ) {
            granted = true;
        }
        else
        {
            requestPermissions(permissions, MY_PERMISSIONS_REQUEST_NUMBER );
        }
    }

    /**
     * OnRequestPermissionsResult override that initializes granted.
     *
     * @param requestCode the request code
     * @param permissions the permissions list that you want to granted
     * @param grantResults the permissions list that has been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        if ( requestCode == 1 && grantResults.length > 0 ) {
            granted = true;
            for ( int grantResult : grantResults ) {
                if ( grantResult != PackageManager.PERMISSION_GRANTED ) {
                    granted = false;
                }
            }
        }
    }


    /**
     * FinalPath setter.
     *
     * @param path the new finalPath
     */
    public void setFinalPath( String path ) {
        this.finalPath = path;
    }

    /**
     * Change file's name.
     *
     * @param oldPath the path with the old name
     * @param newPath the path with the new name
     */
    public boolean renameFile(String oldPath, String newPath ) {
        File oldFile = new File( oldPath );
        File newFile = new File( newPath );

        return oldFile.renameTo( newFile );
    }

    /**
     * Adds a Record in the dataBase
     *
     * @param name the record's name
     * @param filePath the file's path
     */
    public void addRecordDB(String name, String filePath) {
        Record record = new Record( name, filePath);
        manager.add( record );
    }

    /**
     * Saves files.
     */
    public void save() {
        DateFormat dateFormat  = new SimpleDateFormat( "dd-MM-yyyy HH-mm-ss" );
        Date currentDate = new Date( System.currentTimeMillis() );
        fileName = dateFormat.format( currentDate );
        String newPath = FILE_PATH + File.separator + fileName + ".wav";

        if ( renameFile( finalPath, newPath ) ) {
            setFinalPath( newPath );
            addRecordDB( fileName, newPath );
        }
    }

    /**
     * Runs the analyse of the data.
     */
    public void analyseData() {
        AudioData audioData = new AudioData();
        boolean fileOK;
        file = new File(finalPath);

        try {
            if (!file.exists())
                //Ignored we don't need the result because we try to create only if the file doesn't exist.
                file.createNewFile();
                fileOK = true;
        }
        catch (IOException e) {
            Log.e("AnalyseData", e.getMessage(), e);
            fileOK = false;
        }

        if (!fileOK) {
            return;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            int test = inputStream.read();
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);
            System.out.println("b");
            System.out.println(Arrays.toString(b));
            short[] s = new short[(b.length - 44) / 2];
            ByteBuffer.wrap(b)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(s);

            for (short ss : s) {
                audioData.addData(ss);
            }

            audioData.setMaxAmplitudeAbs();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        finally
        {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        audioData.processData();
        FeaturesCalculator featuresCalculator = new FeaturesCalculator(audioData, pitches);
        this.shimmer = featuresCalculator.getShimmer();
        this.jitter = featuresCalculator.getJitter();
        f0 = featuresCalculator.getfundamentalFreq();
        System.out.println("from micro phone");
        System.out.println("shimmer:"+shimmer);
        System.out.println("jitter"+jitter);
        System.out.println("f0:"+f0);


    }

    /**
     * create the wav file
     * @return a wav file object
     * @throws IOException
     */
    private File createWavFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String wavFileName = "NEWLOAD_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File wav = File.createTempFile(
                wavFileName,  /* prefix */
                ".wav",         /* suffix */
                storageDir      /* directory */
        );
        return wav;
    }

    /**
     * copy the stream to a output stream.
     * @param input input stream
     * @param output output stream
     * @throws IOException
     */
    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    /**
     * pitches setter.
     * @param pitches
     */
    public void setPitches(List<Float> pitches) {
        this.pitches = pitches;
    }

    /**
     * Callback function for reading wav files.
     * @param requestCode Request status code
     * @param resultCode Result status code
     * @param data data
     */
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                try {
                    file = createWavFile();
                    InputStream inputStream = getActivity().getContentResolver().openInputStream(data.getData());
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    copyStream(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();

                    finalPath = file.getAbsolutePath();
                    updateView(Status_mic.FILE_FINISH);
                } catch (Exception e) {
                }
            } else {

                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    List<String> pathList = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();

                        pathList.add(uri.toString());
                    }
                }
            }
        }
}
}