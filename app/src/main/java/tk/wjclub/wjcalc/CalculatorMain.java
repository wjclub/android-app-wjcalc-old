package tk.wjclub.wjcalc;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.support.transition.*;
import org.joou.ULong;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class CalculatorMain extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ViewGroup mainView;
    private TextView outputText;
    public enum bitrates {EIGHT,SIXTEEN,THIRTYTWO,SIXTYFOUR;
        public Object getNumberInCurrentEncoding(long number) {
            switch (this){
                case EIGHT:
                    return (byte)number;
                case SIXTEEN:
                    return (short)number;
                case THIRTYTWO:
                    return (int)number;
                case SIXTYFOUR:
                    return (long)number;
            }
            return null;
        }
    };
    private bitrates bitrate = bitrates.SIXTYFOUR;
    private SeekBar bitrateSeekBar;
    private TextView bitrateText;
    private RadioGroup encodingSwitcher;
    private RadioButton encodingHex, encodingDec, encodingOct, encodingBin;
    private long currentNumber, oldCurrentNumber, cachedNumber, memoryNumber = 0L;
    private enum encodings {HEX, DEC, OCT, BIN;
        public int parseToBitsPerChar() {
            switch (this) {
                case HEX:
                    return 16;
                case DEC:
                    return 10;
                case OCT:
                    return 8;
                case BIN:
                    return 2;
            }
            return 0;
        }
    };
    encodings currentEncoding;
    private Button[] numpadButtons = new Button[16];
    private Button currentOperatorButton = null;
    private enum operators {NULL, PLUS, MINUS, MULTIPLY, DIVIDE, OR, NOT, XOR, AND, MOD;

        public static operators parse(int id) {
            switch (id) {
                case R.id.btn_calc_add:
                    return operators.PLUS;
                case R.id.btn_calc_sub:
                    return operators.MINUS;
                case R.id.btn_calc_div:
                    return operators.DIVIDE;
                case R.id.btn_calc_mul:
                    return operators.MULTIPLY;
                case R.id.btn_calc_or:
                    return operators.OR;
                case R.id.btn_calc_xor:
                    return operators.XOR;
                case R.id.btn_calc_and:
                    return operators.AND;
                case R.id.btn_calc_not:
                    return operators.NOT;
                case R.id.btn_calc_mod:
                    return operators.MOD;
            }
            return operators.NULL;
        }
    };
    private operators currentOperator = operators.NULL;
    private long[] bitrateMins = {
            Byte.MIN_VALUE,
            Short.MIN_VALUE,
            Integer.MIN_VALUE,
            Long.MIN_VALUE
    };
    private long[] bitrateMaxes = {
            Byte.MAX_VALUE,
            Short.MAX_VALUE,
            Integer.MAX_VALUE,
            Long.MAX_VALUE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator_main);
        mainView = (ViewGroup) findViewById(R.id.content_constraint_layout);
        currentEncoding = encodings.DEC;
        encodingSwitcher = findViewById(R.id.encoding_switcher);
        encodingSwitcher.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.encoding_hex:
                        currentEncoding = encodings.HEX;
                        break;
                    case R.id.encoding_dec:
                        currentEncoding = encodings.DEC;
                        break;
                    case R.id.encoding_oct:
                        currentEncoding = encodings.OCT;
                        break;
                    case R.id.encoding_bin:
                        currentEncoding = encodings.BIN;
                        break;
                }
                updateNumpad();
                updateNumbers();
            }
        });
        encodingOct = findViewById(R.id.encoding_oct);
        encodingDec = findViewById(R.id.encoding_dec);
        encodingBin = findViewById(R.id.encoding_bin);
        encodingHex = findViewById(R.id.encoding_hex);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_clear:
                        onKeypadClick(findViewById(R.id.btn_calc_ce));
                        return true;
                    case R.id.action_settings:
                        return true;
                }
                return false;
            }
        });
        bitrateText = findViewById(R.id.bitrate_text);
        bitrateSeekBar = findViewById(R.id.bitrate_seeker);
        bitrateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                bitrate = bitrates.values()[progress];
                makeNewNumberConform(oldCurrentNumber);
                bitrateText.setText(String.format("%2d bit", getCorrespondingBitrate(progress)));
                updateNumbers();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                oldCurrentNumber = currentNumber;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        outputText = findViewById(R.id.output_text);
        int[] numpadButtonIDs = {
                R.id.btn_calc_input_0,
                R.id.btn_calc_input_1,
                R.id.btn_calc_input_2,
                R.id.btn_calc_input_3,
                R.id.btn_calc_input_4,
                R.id.btn_calc_input_5,
                R.id.btn_calc_input_6,
                R.id.btn_calc_input_7,
                R.id.btn_calc_input_8,
                R.id.btn_calc_input_9,
                R.id.btn_calc_input_a,
                R.id.btn_calc_input_b,
                R.id.btn_calc_input_c,
                R.id.btn_calc_input_d,
                R.id.btn_calc_input_e,
                R.id.btn_calc_input_f,
        };
        for (int i = 0; i < 16; i++) {
            numpadButtons[i] = (Button) findViewById(numpadButtonIDs[i]);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        onKeypadClick(findViewById(R.id.btn_calc_ce));
    }

    private void makeNewNumberConform(long newNumber) {
        switch (bitrate) {
            case EIGHT:
                currentNumber = (byte) newNumber;
                break;
            case SIXTEEN:
                currentNumber = (short) newNumber;
                break;
            case THIRTYTWO:
                currentNumber = (int) newNumber;
                break;
            case SIXTYFOUR:
                currentNumber = (long) newNumber;
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.calculator_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_normal) {

        } else if (id == R.id.nav_science) {

        } else if (id == R.id.nav_programming) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_about) {
            Intent i = new Intent(CalculatorMain.this, AboutActivity.class);
            startActivity(i);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onKeypadClick(View v) {
        Button currentButton = (Button)v;
        char currentInput = '#';
        switch (v.getId()) {
            //region NUMBERINPUT
            case R.id.btn_calc_input_0:
                currentInput = '0';
                break;
            case R.id.btn_calc_input_1:
                currentInput = '1';
                break;
            case R.id.btn_calc_input_2:
                currentInput = '2';
                break;
            case R.id.btn_calc_input_3:
                currentInput = '3';
                break;
            case R.id.btn_calc_input_4:
                currentInput = '4';
                break;
            case R.id.btn_calc_input_5:
                currentInput = '5';
                break;
            case R.id.btn_calc_input_6:
                currentInput = '6';
                break;
            case R.id.btn_calc_input_7:
                currentInput = '7';
                break;
            case R.id.btn_calc_input_8:
                currentInput = '8';
                break;
            case R.id.btn_calc_input_9:
                currentInput = '9';
                break;
            case R.id.btn_calc_input_a:
                currentInput = 'a';
                break;
            case R.id.btn_calc_input_b:
                currentInput = 'b';
                break;
            case R.id.btn_calc_input_c:
                currentInput = 'c';
                break;
            case R.id.btn_calc_input_d:
                currentInput = 'd';
                break;
            case R.id.btn_calc_input_e:
                currentInput = 'e';
                break;
            case R.id.btn_calc_input_f:
                currentInput = 'f';
                break;
            //endregion
            //region OPERATORS
            case R.id.btn_calc_add:
            case R.id.btn_calc_sub:
            case R.id.btn_calc_div:
            case R.id.btn_calc_mul:
            case R.id.btn_calc_or:
            case R.id.btn_calc_xor:
            case R.id.btn_calc_and:
            case R.id.btn_calc_not:
            case R.id.btn_calc_mod:
                //On Operator Button Click
                Button oldCurrentOperatorButton = currentOperatorButton;
                if (currentOperatorButton != null) {
                    currentOperatorButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.button_background)));
                    currentOperatorButton.setTextColor(Color.BLACK);
                    currentOperatorButton = null;
                }
                if (currentButton != oldCurrentOperatorButton) {
                    currentButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                    currentButton.setTextColor(Color.WHITE);
                    currentOperator = operators.parse(currentButton.getId());
                    currentOperatorButton = currentButton;
                }
                if (currentOperatorButton != null) {
                    currentOperator = operators.parse(currentOperatorButton.getId());
                } else {
                    currentOperator = operators.NULL;
                }
                break;
                //endregion
            case R.id.btn_calc_input_pm:
                //Negate Button
                currentNumber = (-1) * currentNumber;
                break;
                //region DELETING BTN
            case R.id.btn_calc_ce:
                currentNumber = 0L;
                oldCurrentNumber = 0L;
                cachedNumber = 0L;
                currentOperator = operators.NULL;
                break;
            case R.id.btn_calc_c:
                currentNumber = 0L;

                break;
            case R.id.btn_calc_del:
                //Delete Button
                String stuff = Long.toString(currentNumber, currentEncoding.parseToBitsPerChar());
                if (stuff.length() > 1)
                    currentNumber = Long.parseLong(stuff.substring(0, stuff.length()-1));
                else
                    currentNumber = 0;
                break;
                //endregion
            case R.id.btn_calc_exec:
                //TODO Make it calculate
                break;
        }
        if (currentInput != '#') {
            String currentNumberInput = Long.toString(currentNumber, currentEncoding.parseToBitsPerChar()) + currentInput;
            byte[] stuff = null;
            BigInteger newNumber = null;
            try {
                newNumber = new BigInteger(currentNumberInput, currentEncoding.parseToBitsPerChar());
                switch (bitrate) {
                    case EIGHT:
                        if (newNumber.bitLength() < 8)
                            currentNumber = newNumber.byteValue();
                        break;
                    case SIXTEEN:
                        if (newNumber.bitLength() < 16)
                            currentNumber = newNumber.shortValue();
                        break;
                    case THIRTYTWO:
                        if (newNumber.bitLength() < 32)
                            currentNumber = newNumber.intValue();
                        break;
                    case SIXTYFOUR:
                        if (newNumber.bitLength() < 64)
                            currentNumber = newNumber.longValue();
                        break;
                }
            } catch (NumberFormatException e) {
                //TODO Spawn Snackbar or something
            }
        }
        updateNumbers();
        updateNumpad();
    }

    private String getCurrentNumberInCurrentFormat() {
        return Long.toString(currentNumber, currentEncoding.parseToBitsPerChar());
    }

    private void updateNumbers() {
        outputText.setText(getCurrentNumberInCurrentFormat().toUpperCase());
        switch (bitrate) {
            case EIGHT:
                encodingHex.setText(Html.fromHtml("HEX " + Integer.toHexString(((byte)currentNumber) & 0xFF).toUpperCase()));
                encodingDec.setText(Html.fromHtml("DEC " + Byte.toString((byte)currentNumber)));
                encodingOct.setText(Html.fromHtml("OCT " + Integer.toOctalString(((byte)currentNumber) & 0xFF).toUpperCase()));
                encodingBin.setText(Html.fromHtml("BIN " + Integer.toBinaryString(((byte)currentNumber) & 0xFF).toUpperCase()));
                break;
            case SIXTEEN:
                encodingHex.setText(Html.fromHtml("HEX " + Integer.toHexString(((short)currentNumber) & 0xFFFF).toUpperCase()));
                encodingDec.setText(Html.fromHtml("DEC " + Short.toString((short)currentNumber)));
                encodingOct.setText(Html.fromHtml("OCT " + Integer.toOctalString(((short)currentNumber) & 0xFFFF).toUpperCase()));
                encodingBin.setText(Html.fromHtml("BIN " + Integer.toBinaryString(((short)currentNumber) & 0xFFFF).toUpperCase()));
                break;
            case THIRTYTWO:
                encodingHex.setText(Html.fromHtml("HEX " + Integer.toHexString((int)currentNumber).toUpperCase()));
                encodingDec.setText(Html.fromHtml("DEC " + Integer.toString((int)currentNumber)));
                encodingOct.setText(Html.fromHtml("OCT " + Integer.toOctalString((int)currentNumber)));
                encodingBin.setText(Html.fromHtml("BIN " + Integer.toBinaryString((int)currentNumber)));
                break;
            case SIXTYFOUR:
                encodingHex.setText(Html.fromHtml("HEX " + Long.toHexString(currentNumber).toUpperCase()));
                encodingDec.setText(Html.fromHtml("DEC " + Long.toString(currentNumber)));
                encodingOct.setText(Html.fromHtml("OCT " + Long.toOctalString(currentNumber)));
                encodingBin.setText(Html.fromHtml("BIN " + Long.toBinaryString(currentNumber)));
                break;
        }
    }

    private void updateNumpad() {
        TransitionManager.beginDelayedTransition(mainView);
        for (int i = currentEncoding.parseToBitsPerChar(); i < 16; i++) {
            numpadButtons[i].setEnabled(false);
        }
        for (int i = 0; i < currentEncoding.parseToBitsPerChar(); i++) {
            numpadButtons[i].setEnabled(true);
        }

    }

    private int getCorrespondingBitrate(int i) {
        return (int) (8*Math.pow(2, bitrates.values()[i].ordinal()));
    }

    private String substringIfApplicable(String input, int length) {
        return input.substring(Math.min(input.length(), length));
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}