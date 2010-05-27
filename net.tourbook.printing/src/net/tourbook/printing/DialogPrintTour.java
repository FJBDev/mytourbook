/*******************************************************************************
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.printing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.transform.TransformerException;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.FileCollisionBehavior;
import net.tourbook.ui.ImageComboLabel;
import net.tourbook.ui.UI;

import org.apache.fop.apps.FOPException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;

public class DialogPrintTour extends TitleAreaDialog {

	private static final String				UI_AVERAGE_SYMBOL			= "� ";											//$NON-NLS-1$

	private static final int				VERTICAL_SECTION_MARGIN		= 10;
	private static final int				SIZING_TEXT_FIELD_WIDTH		= 250;
	private static final int				COMBO_HISTORY_LENGTH		= 20;

	private static final String				STATE_IS_PRINT_TOUR_RANGE	= "isPrintTourRange";							//$NON-NLS-1$
	private static final String				STATE_IS_PRINT_MARKERS		= "isPrintMarkers";								//$NON-NLS-1$
	private static final String				STATE_IS_PRINT_NOTES		= "isPrintNotes";								//$NON-NLS-1$

	private static final String				STATE_IS_CAMOUFLAGE_SPEED	= "isCamouflageSpeed";							//$NON-NLS-1$
	private static final String				STATE_CAMOUFLAGE_SPEED		= "camouflageSpeedValue";						//$NON-NLS-1$

	private static final String				STATE_PRINT_PATH_NAME		= "printPathName";								//$NON-NLS-1$
	private static final String				STATE_PRINT_FILE_NAME		= "printtFileName";								//$NON-NLS-1$
	private static final String				STATE_IS_OVERWRITE_FILES	= "isOverwriteFiles";							//$NON-NLS-1$

	private static final DecimalFormat		_intFormatter				= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final DecimalFormat		_double2Formatter			= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final DecimalFormat		_double6Formatter			= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final SimpleDateFormat	_dateFormat					= new SimpleDateFormat();
	private static final DateFormat			_timeFormatter				= DateFormat.getTimeInstance(DateFormat.MEDIUM);
	private static final NumberFormat		_numberFormatter			= NumberFormat.getNumberInstance();

	private static final String				PDF_FILE_EXTENSION			= "pdf";

	private static String					_dlgDefaultMessage;

	static {
		_intFormatter.applyPattern("000000"); //$NON-NLS-1$
		_double2Formatter.applyPattern("0.00"); //$NON-NLS-1$
		_double6Formatter.applyPattern("0.0000000"); //$NON-NLS-1$
		_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
	}

	private final IDialogSettings			_state						= TourbookPlugin
																				.getDefault()
																				.getDialogSettingsSection(
																						"DialogPrintTour");				//$NON-NLS-1$

	private final PrintTourExtension		_printExtensionPoint;

	private final ArrayList<TourData>		_tourDataList;
	private final int						_tourStartIndex;
	private final int						_tourEndIndex;

	private Point							_shellDefaultSize;
	private Composite						_dlgContainer;

	private Button							_chkPrintTourRange;
	private Button							_chkPrintMarkers;
	private Button							_chkPrinttNotes;

	private Button							_chkCamouflageSpeed;
	private Text							_txtCamouflageSpeed;
	private Label							_lblCoumouflageSpeedUnit;

	private Composite						_inputContainer;

	private Combo							_comboFile;
	private Combo							_comboPath;
	private Button							_btnSelectFile;
	private Button							_btnSelectDirectory;
	private Text							_txtFilePath;
	private Button							_chkOverwriteFiles;

	private ProgressIndicator				_progressIndicator;
	private ImageComboLabel					_lblPrintFilePath;

	private boolean							_isInit;

	public DialogPrintTour(	final Shell parentShell,
							final PrintTourExtension printExtensionPoint,
							final ArrayList<TourData> tourDataList,
							final int tourStartIndex,
							final int tourEndIndex) {

		super(parentShell);

		int shellStyle = getShellStyle();

		shellStyle = //
		SWT.NONE //
				| SWT.TITLE
				| SWT.CLOSE
				| SWT.MIN
//				| SWT.MAX
				| SWT.RESIZE
				| SWT.NONE;

		// make dialog resizable
		setShellStyle(shellStyle);

		_printExtensionPoint = printExtensionPoint;
		_tourDataList = tourDataList;
		_tourStartIndex = tourStartIndex;
		_tourEndIndex = tourEndIndex;

		_dlgDefaultMessage = NLS.bind(Messages.Dialog_Print_Dialog_Message, _printExtensionPoint.getVisibleName());

	}

	/**
	 * @return Returns <code>true</code> when a part of a tour can be printed
	 */
	private boolean canPrintTourPart() {
		return (_tourDataList.size() == 1) && (_tourStartIndex >= 0) && (_tourEndIndex > 0);
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_Print_Shell_Text);

		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {

				// allow resizing the width but not the height

				if (_shellDefaultSize == null) {
					_shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				}

				final Point shellSize = shell.getSize();

				/*
				 * this is not working, the shell is flickering when the shell size is below min
				 * size and I found no way to prevent a resize :-(
				 */
//				if (shellSize.x < _shellDefaultSize.x) {
//					event.doit = false;
//				}

				shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
				shellSize.y = _shellDefaultSize.y;

				shell.setSize(shellSize);
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_Print_Dialog_Title);
		setMessage(_dlgDefaultMessage);

		_isInit = true;
		{
			restoreState();
		}
		_isInit = false;

		setFileName();
		validateFields();
		enableFields();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_Print_Btn_Print);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(_dlgContainer);

		return _dlgContainer;
	}

	private void createUI(final Composite parent) {

		_inputContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
		GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);

		createUIOption(_inputContainer);
		createUIDestination(_inputContainer);
		createUIProgress(parent);
	}

	private void createUIDestination(final Composite parent) {

		Label label;

		final ModifyListener filePathModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateFields();
			}
		};

		/*
		 * group: filename
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Print_Group_PdfFileName);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		{
			/*
			 * label: filename
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_Print_Label_FileName);

			/*
			 * combo: path
			 */
			_comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
			((GridData) _comboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			_comboFile.setVisibleItemCount(20);
			_comboFile.addVerifyListener(net.tourbook.util.UI.verifyFilenameInput());
			_comboFile.addModifyListener(filePathModifyListener);
			_comboFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectFile = new Button(group, SWT.PUSH);
			_btnSelectFile.setText(Messages.app_btn_browse);
			_btnSelectFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseFile();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectFile);

			// -----------------------------------------------------------------------------

			/*
			 * label: path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_Print_Label_PrintFilePath);

			/*
			 * combo: path
			 */
			_comboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPath);
			((GridData) _comboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			_comboPath.setVisibleItemCount(20);
			_comboPath.addModifyListener(filePathModifyListener);
			_comboPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectDirectory = new Button(group, SWT.PUSH);
			_btnSelectDirectory.setText(Messages.app_btn_browse);
			_btnSelectDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectDirectory);

			// -----------------------------------------------------------------------------

			/*
			 * checkbox: overwrite files
			 */
			_chkOverwriteFiles = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).span(3, 1).applyTo(_chkOverwriteFiles);
			_chkOverwriteFiles.setText(Messages.Dialog_Print_Chk_OverwriteFiles);
			_chkOverwriteFiles.setToolTipText(Messages.Dialog_Print_Chk_OverwriteFiles_Tooltip);

			// -----------------------------------------------------------------------------

			/*
			 * label: file path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_Print_Label_FilePath);

			/*
			 * text: filename
			 */
			_txtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
			_txtFilePath.setToolTipText(Messages.Dialog_Print_Txt_FilePath_Tooltip);
			_txtFilePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			// spacer
//			new Label(group, SWT.NONE);
		}

	}

	private void createUIOption(final Composite parent) {

		// container
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Print_Group_Options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			createUIOptionCamouflageSpeed(group);
			createUIOptionPrintMarkers(group);
			createUIOptionPrintNotes(group);
			createUIOptionTourPart(group);
		}
	}

	private void createUIOptionCamouflageSpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * checkbox: camouflage speed
			 */
			_chkCamouflageSpeed = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkCamouflageSpeed);
			_chkCamouflageSpeed.setText(Messages.Dialog_Print_Chk_CamouflageSpeed);
			_chkCamouflageSpeed.setToolTipText(Messages.Dialog_Print_Chk_CamouflageSpeed_Tooltip);
			_chkCamouflageSpeed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {

					validateFields();
					enableFields();

					if (_chkCamouflageSpeed.getSelection()) {
						_txtCamouflageSpeed.setFocus();
					}
				}
			});

			// text: speed
			_txtCamouflageSpeed = new Text(container, SWT.BORDER | SWT.TRAIL);
			_txtCamouflageSpeed.setToolTipText(Messages.Dialog_Print_Chk_CamouflageSpeedInput_Tooltip);
			_txtCamouflageSpeed.addModifyListener(new ModifyListener() {
				public void modifyText(final ModifyEvent e) {
					validateFields();
					enableFields();
				}
			});
			_txtCamouflageSpeed.addListener(SWT.Verify, new Listener() {
				public void handleEvent(final Event e) {
					net.tourbook.util.UI.verifyIntegerInput(e, false);
				}
			});

			// label: unit
			_lblCoumouflageSpeedUnit = new Label(container, SWT.NONE);
			_lblCoumouflageSpeedUnit.setText(UI_AVERAGE_SYMBOL + UI.UNIT_LABEL_SPEED);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(
					_lblCoumouflageSpeedUnit);
		}
	}

	private void createUIOptionPrintMarkers(final Composite parent) {

		/*
		 * checkbox: print markers
		 */
		_chkPrintMarkers = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkPrintMarkers);
		_chkPrintMarkers.setText(Messages.Dialog_Print_Chk_PrintMarkers);
		_chkPrintMarkers.setToolTipText(Messages.Dialog_Print_Chk_PrintMarkers_Tooltip);
	}

	private void createUIOptionPrintNotes(final Composite parent) {

		/*
		 * checkbox: print notes
		 */
		_chkPrinttNotes = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkPrinttNotes);
		_chkPrinttNotes.setText(Messages.Dialog_Print_Chk_PrintNotes);
		_chkPrinttNotes.setToolTipText(Messages.Dialog_Print_Chk_PrintNotes_Tooltip);
	}

	private void createUIOptionTourPart(final Composite parent) {

		/*
		 * checkbox: tour range
		 */
		String tourRangeUI = null;

		if ((_tourDataList.size() == 1) && (_tourStartIndex != -1) && (_tourEndIndex != -1)) {

			final TourData tourData = _tourDataList.get(0);
			final int[] timeSerie = tourData.timeSerie;
			if (timeSerie != null) {

				final int[] distanceSerie = tourData.distanceSerie;
				final boolean isDistance = distanceSerie != null;

				final int startTime = timeSerie[_tourStartIndex];
				final int endTime = timeSerie[_tourEndIndex];

				final DateTime dtTour = new DateTime(
						tourData.getStartYear(),
						tourData.getStartMonth() - 1,
						tourData.getStartDay(),
						tourData.getStartHour(),
						tourData.getStartMinute(),
						tourData.getStartSecond(),
						0);

				final String uiStartTime = _timeFormatter.format(dtTour.plusSeconds(startTime).toDate());
				final String uiEndTime = _timeFormatter.format(dtTour.plusSeconds(endTime).toDate());

				if (isDistance) {

					_numberFormatter.setMinimumFractionDigits(3);
					_numberFormatter.setMaximumFractionDigits(3);

					tourRangeUI = NLS.bind(Messages.Dialog_Print_Chk_TourRangeWithDistance, new Object[] {
							uiStartTime,
							uiEndTime,

							_numberFormatter.format(((float) distanceSerie[_tourStartIndex])
									/ 1000
									/ UI.UNIT_VALUE_DISTANCE),

							_numberFormatter.format(((float) distanceSerie[_tourEndIndex])
									/ 1000
									/ UI.UNIT_VALUE_DISTANCE),

							UI.UNIT_LABEL_DISTANCE,

							// adjust by 1 to corresponds to the number in the tour editor
							_tourStartIndex + 1,
							_tourEndIndex + 1 });

				} else {

					tourRangeUI = NLS.bind(Messages.Dialog_Print_Chk_TourRangeWithoutDistance, new Object[] {
							uiStartTime,
							uiEndTime,
							_tourStartIndex + 1,
							_tourEndIndex + 1 });
				}
			}
		}

		_chkPrintTourRange = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkPrintTourRange);

		_chkPrintTourRange.setText(tourRangeUI != null ? tourRangeUI : Messages.Dialog_Print_Chk_TourRangeDisabled);

		_chkPrintTourRange.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableFields();
			}
		});
	}

	private void createUIProgress(final Composite parent) {

		final int selectedTours = _tourDataList.size();

		// hide progress bar when only one tour is printed
		if (selectedTours < 2) {
			return;
		}

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(container);
		GridLayoutFactory.swtDefaults().margins(10, 5).numColumns(1).applyTo(container);
		{
			/*
			 * progress indicator
			 */
			_progressIndicator = new ProgressIndicator(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_progressIndicator);

			/*
			 * label: printed filename
			 */
			_lblPrintFilePath = new ImageComboLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblPrintFilePath);
		}
	}

	private void doPrint() throws IOException {

		// disable button's
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

		final String completeFilePath = _txtFilePath.getText();

		final boolean isOverwriteFiles = _chkOverwriteFiles.getSelection();
		final boolean isCamouflageSpeed = _chkCamouflageSpeed.getSelection();
		final float[] camouflageSpeed = new float[1];
		try {
			camouflageSpeed[0] = Float.parseFloat(_txtCamouflageSpeed.getText());
		} catch (final NumberFormatException e) {
			camouflageSpeed[0] = 0.1F;
		}
		camouflageSpeed[0] *= UI.UNIT_VALUE_DISTANCE / 3.6f;

		final FileCollisionBehavior fileCollisionBehaviour = new FileCollisionBehavior();

		if (_tourDataList.size() == 1) {
			// print one tour
			final TourData tourData = _tourDataList.get(0);
			
			if (_printExtensionPoint instanceof PrintTourPDF) {
				try {
					//TODO handle exception
					//System.out.println("tour id:"+tourData.getTourId());	
					((PrintTourPDF)_printExtensionPoint).printPDF(tourData, completeFilePath);
				} catch (FOPException e) {
					e.printStackTrace();
				} catch (TransformerException e) {
					e.printStackTrace();
				}
			}
		} else {
			/*
			 * print each tour separately
			 */

			final String printPathName = getPrintPathName();
			_progressIndicator.beginTask(_tourDataList.size());

			final Job printJob = new Job("print tours") { //$NON-NLS-1$
				@Override
				public IStatus run(final IProgressMonitor monitor) {

					monitor.beginTask(UI.EMPTY_STRING, _tourDataList.size());
					final IPath printFilePath = new Path(printPathName).addTrailingSeparator();

					for (final TourData tourData : _tourDataList) {

						// get filepath
						final IPath filePath = printFilePath
								.append(UI.format_yyyymmdd_hhmmss(tourData))
								.addFileExtension(PDF_FILE_EXTENSION);

						/*
						 *	print: update dialog progress monitor
						 */
						Display.getDefault().syncExec(new Runnable() {
							public void run() {

								// display printed filepath
								_lblPrintFilePath.setText(NLS.bind(Messages.Dialog_Print_Lbl_PdfFilePath, filePath
										.toOSString()));

								// !!! force label update !!!
								_lblPrintFilePath.update();

								_progressIndicator.worked(1);

							}
						});

						
						if (_printExtensionPoint instanceof PrintTourPDF) {
							try {
								//TODO handle exception
								((PrintTourPDF)_printExtensionPoint).printPDF(tourData, filePath.toOSString());
							} catch (FOPException e) {
								e.printStackTrace();
							} catch (TransformerException e) {
								e.printStackTrace();
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
						}			
						
						// check if overwrite dialog was canceled
						if (fileCollisionBehaviour.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
							break;
						}
					}

					return Status.OK_STATUS;
				}
			};

			printJob.schedule();
			try {
				printJob.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		MessageDialog.openInformation(getShell(), "Not yet implemented", "Beware, printing is not yet fully implemented.");
	}

	private void enableExportButton(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {

			okButton.setEnabled(isEnabled);
		}
	}

	private void enableFields() {

		_comboFile.setEnabled(true);

		final boolean isCamouflageTime = _chkCamouflageSpeed.getSelection();
		_txtCamouflageSpeed.setEnabled(isCamouflageTime);
		_lblCoumouflageSpeedUnit.setEnabled(isCamouflageTime);

		_chkPrintTourRange.setEnabled(canPrintTourPart());
		// when disabled, uncheck it
		if (_chkPrintTourRange.isEnabled() == false) {
			_chkPrintTourRange.setSelection(false);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return _state;
	}

	private String getPrintFileName() {
		return _comboFile.getText().trim();
	}

	private String getPrintPathName() {
		return _comboPath.getText().trim();
	}

	private String[] getUniqueItems(final String[] pathItems, final String currentItem) {

		final ArrayList<String> pathList = new ArrayList<String>();

		pathList.add(currentItem);

		for (final String pathItem : pathItems) {

			// ignore duplicate entries
			if (currentItem.equals(pathItem) == false) {
				pathList.add(pathItem);
			}

			if (pathList.size() >= COMBO_HISTORY_LENGTH) {
				break;
			}
		}

		return pathList.toArray(new String[pathList.size()]);
	}

	/**
	 * @return Return <code>true</code> when a part of a tour can be printed
	 */
	private boolean isPrintTourPart() {

		final boolean[] result = new boolean[1];

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				result[0] = _chkPrintTourRange.getSelection()
						&& (_tourDataList.size() == 1)
						&& (_tourStartIndex != -1)
						&& (_tourEndIndex != -1);
			}
		});

		return result[0];
	}

	@Override
	protected void okPressed() {

		UI.disableAllControls(_inputContainer);

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					doPrint();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		});

		super.okPressed();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(_dlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.Dialog_Print_Dir_Dialog_Text);
		dialog.setMessage(Messages.Dialog_Print_Dir_Dialog_Message);

		dialog.setFilterPath(getPrintPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			_comboPath.setText(selectedDirectoryName);
		}
	}

	private void onSelectBrowseFile() {

		final String fileExtension = PDF_FILE_EXTENSION;

		final FileDialog dialog = new FileDialog(_dlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.Dialog_Print_File_Dialog_Text);

		dialog.setFilterPath(getPrintPathName());
		dialog.setFilterExtensions(new String[] { fileExtension });
		dialog.setFileName("*." + fileExtension);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();

		if (selectedFilePath != null) {
			setErrorMessage(null);
			_comboFile.setText(new Path(selectedFilePath).toFile().getName());
		}
	}

	private void restoreState() {
		_chkPrintTourRange.setSelection(_state.getBoolean(STATE_IS_PRINT_TOUR_RANGE));
		_chkPrintMarkers.setSelection(_state.getBoolean(STATE_IS_PRINT_MARKERS));
		_chkPrinttNotes.setSelection(_state.getBoolean(STATE_IS_PRINT_NOTES));

		// camouflage speed
		_chkCamouflageSpeed.setSelection(_state.getBoolean(STATE_IS_CAMOUFLAGE_SPEED));
		final String camouflageSpeed = _state.get(STATE_CAMOUFLAGE_SPEED);
		_txtCamouflageSpeed.setText(camouflageSpeed == null ? "10" : camouflageSpeed);//$NON-NLS-1$
		_txtCamouflageSpeed.selectAll();

		// export file/path
		UI.restoreCombo(_comboFile, _state.getArray(STATE_PRINT_FILE_NAME));
		UI.restoreCombo(_comboPath, _state.getArray(STATE_PRINT_PATH_NAME));
		_chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_FILES));
	}

	private void saveState() {

		// export file/path
		if (validateFilePath()) {
			_state.put(STATE_PRINT_PATH_NAME, getUniqueItems(_comboPath.getItems(), getPrintPathName()));
			_state.put(STATE_PRINT_FILE_NAME, getUniqueItems(_comboFile.getItems(), getPrintFileName()));
		}

		// export tour part
		if (canPrintTourPart()) {
			_state.put(STATE_IS_PRINT_TOUR_RANGE, _chkPrintTourRange.getSelection());
		}

		// camouflage speed
		_state.put(STATE_IS_CAMOUFLAGE_SPEED, _chkCamouflageSpeed.getSelection());
		_state.put(STATE_CAMOUFLAGE_SPEED, _txtCamouflageSpeed.getText());

		_state.put(STATE_IS_OVERWRITE_FILES, _chkOverwriteFiles.getSelection());
		_state.put(STATE_IS_PRINT_MARKERS, _chkPrintMarkers.getSelection());
		_state.put(STATE_IS_PRINT_NOTES, _chkPrinttNotes.getSelection());
	}

	private void setError(final String message) {
		setErrorMessage(message);
		enableExportButton(false);
	}

	/**
	 * Overwrite filename with the first tour date/time when the tour is not merged
	 */
	private void setFileName() {

		// search for the first tour
		TourData minTourData = null;
		final long minTourMillis = 0;

		for (final TourData tourData : _tourDataList) {
			final DateTime checkingTourDate = TourManager.getTourDateTime(tourData);

			if (minTourData == null) {
				minTourData = tourData;
			} else {

				final long tourMillis = checkingTourDate.getMillis();
				if (tourMillis < minTourMillis) {
					minTourData = tourData;
				}
			}
		}

		if ((_tourDataList.size() == 1) && (_tourStartIndex != -1) && (_tourEndIndex != -1)) {

			// display the start date/time

			final DateTime dtTour = new DateTime(minTourData.getStartYear(), minTourData.getStartMonth(), minTourData
					.getStartDay(), minTourData.getStartHour(), minTourData.getStartMinute(), minTourData
					.getStartSecond(), 0);

			final int startTime = minTourData.timeSerie[_tourStartIndex];
			final DateTime tourTime = dtTour.plusSeconds(startTime);

			_comboFile.setText(UI
					.format_yyyymmdd_hhmmss(
							tourTime.getYear(),
							tourTime.getMonthOfYear(),
							tourTime.getDayOfMonth(),
							tourTime.getHourOfDay(),
							tourTime.getMinuteOfHour(),
							tourTime.getSecondOfMinute()));
		} else {

			// display the tour date/time

			_comboFile.setText(UI.format_yyyymmdd_hhmmss(minTourData));
		}
	}

	private void validateFields() {

		if (_isInit) {
			return;
		}

		/*
		 * validate fields
		 */

		if (validateFilePath() == false) {
			return;
		}

		// speed value
		final boolean isEqualizeTimeEnabled = _chkCamouflageSpeed.getSelection();
		if (isEqualizeTimeEnabled) {

			if (net.tourbook.util.UI.verifyIntegerValue(_txtCamouflageSpeed.getText()) == false) {
				setError(Messages.Dialog_Print_Error_CamouflageSpeedIsInvalid);
				_txtCamouflageSpeed.setFocus();
				return;
			}
		}

		setErrorMessage(null);
		enableExportButton(true);
	}

	private boolean validateFilePath() {

		// check path
		IPath filePath = new Path(getPrintPathName());
		if (new File(filePath.toOSString()).exists() == false) {

			// invalid path
			setError(NLS.bind(Messages.Dialog_Print_Msg_PathIsNotAvailable, filePath.toOSString()));
			return false;
		}

		boolean returnValue = false;

		String fileName = getPrintFileName();

		// remove extentions
		final int extPos = fileName.indexOf('.');
		if (extPos != -1) {
			fileName = fileName.substring(0, extPos);
		}

		// build file path with extension
		filePath = filePath.addTrailingSeparator().append(fileName).addFileExtension(PDF_FILE_EXTENSION);

		final File newFile = new File(filePath.toOSString());

		if ((fileName.length() == 0) || newFile.isDirectory()) {

			// invalid filename

			setError(Messages.Dialog_Print_Msg_FileNameIsInvalid);

		} else if (newFile.exists()) {
			
			// file already exists

			setMessage(
					NLS.bind(Messages.Dialog_Print_Msg_FileAlreadyExists, filePath.toOSString()),
					IMessageProvider.WARNING);
			returnValue = true;

		} else {

			setMessage(_dlgDefaultMessage);

			try {
				final boolean isFileCreated = newFile.createNewFile();

				// name is correct

				if (isFileCreated) {
					// delete file because the file is created for checking validity
					newFile.delete();
				}
				returnValue = true;

			} catch (final IOException ioe) {
				setError(Messages.Dialog_Print_Msg_FileNameIsInvalid);
			}

		}

		_txtFilePath.setText(filePath.toOSString());

		return returnValue;
	}
}
