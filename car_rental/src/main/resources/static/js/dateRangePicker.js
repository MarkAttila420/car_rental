/**
 * Reusable Date Range Picker Component
 * 
 * Usage:
 * const picker = new DateRangePicker({
 *     triggerElementId: 'dateRangeTrigger',
 *     startDateInputId: 'startDate',
 *     endDateInputId: 'endDate',
 *     displayElementId: 'dateRangeDisplay',
 *     pickerContainerId: 'dateRangePicker',
 *     calendar1Id: 'calendar1',
 *     calendar2Id: 'calendar2',
 *     prevMonthButtonId: 'prevMonth',
 *     nextMonthButtonId: 'nextMonth',
 *     onDateRangeChange: function(startDate, endDate) {
 *         // Custom callback when date range changes
 *     }
 * });
 */

class DateRangePicker {
    constructor(config) {
        // Configuration
        this.triggerElementId = config.triggerElementId;
        this.startDateInputId = config.startDateInputId;
        this.endDateInputId = config.endDateInputId;
        this.displayElementId = config.displayElementId;
        this.pickerContainerId = config.pickerContainerId;
        this.calendar1Id = config.calendar1Id;
        this.calendar2Id = config.calendar2Id;
        this.prevMonthButtonId = config.prevMonthButtonId;
        this.nextMonthButtonId = config.nextMonthButtonId;
        this.confirmButtonId = config.confirmButtonId || null;
        this.onDateRangeChange = config.onDateRangeChange || null;
        this.onConfirm = config.onConfirm || null;
        this.defaultDisplayText = config.defaultDisplayText || 'Select date range';
        this.disabledDates = config.disabledDates || []; // Array of date strings in 'YYYY-MM-DD' format
        
        // State
        this.startDate = null;
        this.endDate = null;
        this.isSelectingRange = false;
        this.currentMonthOffset = 0;
        
        this.today = new Date();
        this.today.setHours(0, 0, 0, 0);
        
        this.init();
    }
    
    init() {
        // Initialize with existing values if any
        const initialStart = document.getElementById(this.startDateInputId).value;
        const initialEnd = document.getElementById(this.endDateInputId).value;
        
        if (initialStart) {
            this.startDate = new Date(initialStart);
            this.startDate.setHours(0, 0, 0, 0);
        }
        if (initialEnd) {
            this.endDate = new Date(initialEnd);
            this.endDate.setHours(0, 0, 0, 0);
        }
        
        this.setupEventListeners();
        this.updateDisplay();
    }
    
    setupEventListeners() {
        // Toggle date picker
        document.getElementById(this.triggerElementId).addEventListener('click', (e) => {
            e.stopPropagation();
            this.togglePicker();
        });
        
        // Month navigation
        document.getElementById(this.prevMonthButtonId).addEventListener('click', (e) => {
            e.stopPropagation();
            this.navigateMonth(-1);
        });
        
        document.getElementById(this.nextMonthButtonId).addEventListener('click', (e) => {
            e.stopPropagation();
            this.navigateMonth(1);
        });
        
        // Prevent picker from closing when clicking inside it
        document.getElementById(this.pickerContainerId).addEventListener('click', (e) => {
            e.stopPropagation();
        });
        
        // Handle date selection
        document.getElementById(this.pickerContainerId).addEventListener('click', (e) => {
            this.handleDateClick(e);
        });
        
        // Confirm button
        if (this.confirmButtonId) {
            document.getElementById(this.confirmButtonId).addEventListener('click', (e) => {
                e.stopPropagation();
                this.confirmSelection();
            });
        }
        
        // Close picker when clicking outside
        document.addEventListener('click', (e) => {
            this.handleOutsideClick(e);
        });
    }
    
    togglePicker() {
        const picker = document.getElementById(this.pickerContainerId);
        if (picker.style.display === 'block') {
            picker.style.display = 'none';
        } else {
            this.currentMonthOffset = 0; // Reset to current month when opening
            this.renderCalendars();
            picker.style.display = 'block';
        }
    }
    
    navigateMonth(direction) {
        if (direction < 0 && this.currentMonthOffset > 0) {
            this.currentMonthOffset--;
            this.renderCalendars();
        } else if (direction > 0) {
            this.currentMonthOffset++;
            this.renderCalendars();
        }
    }
    
    handleDateClick(e) {
        if (e.target.classList.contains('calendar-day') && 
            !e.target.classList.contains('disabled') && 
            !e.target.classList.contains('empty')) {
            
            const dateStr = e.target.getAttribute('data-date');
            const clickedDate = new Date(dateStr);
            clickedDate.setHours(0, 0, 0, 0);
            
            if (!this.startDate || (this.startDate && this.endDate)) {
                // Start new selection
                this.startDate = clickedDate;
                this.endDate = null;
                this.isSelectingRange = true;
            } else if (this.startDate && !this.endDate) {
                // Complete the range - check if any dates in between are disabled
                let rangeStart, rangeEnd;
                if (clickedDate < this.startDate) {
                    rangeStart = clickedDate;
                    rangeEnd = this.startDate;
                } else {
                    rangeStart = this.startDate;
                    rangeEnd = clickedDate;
                }
                
                // Check if there are any disabled dates in the range
                if (this.hasDisabledDatesInRange(rangeStart, rangeEnd)) {
                    // Don't allow selection, reset to the clicked date
                    this.startDate = clickedDate;
                    this.endDate = null;
                    this.isSelectingRange = true;
                } else {
                    // Complete the range normally
                    if (clickedDate < this.startDate) {
                        this.endDate = this.startDate;
                        this.startDate = clickedDate;
                    } else {
                        this.endDate = clickedDate;
                    }
                    this.isSelectingRange = false;
                }
            }
            
            this.updateDisplay();
            this.renderCalendars();
            this.updateConfirmButton();
        }
    }
    
    handleOutsideClick(e) {
        const trigger = document.getElementById(this.triggerElementId);
        const picker = document.getElementById(this.pickerContainerId);
        if (!trigger.contains(e.target) && !picker.contains(e.target)) {
            picker.style.display = 'none';
        }
    }
    
    generateCalendar(containerId, date) {
        const year = date.getFullYear();
        const month = date.getMonth();
        
        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const startingDayOfWeek = firstDay.getDay();
        
        const monthNames = ["January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"];
        
        let html = '<div class="calendar">';
        html += '<div class="calendar-header">' + monthNames[month] + ' ' + year + '</div>';
        html += '<div class="calendar-grid">';
        
        // Day headers
        const dayHeaders = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
        dayHeaders.forEach(day => {
            html += '<div class="calendar-day-header">' + day + '</div>';
        });
        
        // Empty cells before first day
        for (let i = 0; i < startingDayOfWeek; i++) {
            html += '<div class="calendar-day empty"></div>';
        }
        
        // Days of month
        for (let day = 1; day <= lastDay.getDate(); day++) {
            const currentDate = new Date(year, month, day);
            currentDate.setHours(0, 0, 0, 0);
            let classes = 'calendar-day';
            
            const dateStr = this.formatDate(currentDate);
            
            if (currentDate < this.today || this.disabledDates.includes(dateStr)) {
                classes += ' disabled';
            }
            
            // Check if this day matches start or end date
            if (this.startDate && currentDate.getTime() === this.startDate.getTime()) {
                classes += ' selected-start';
            }
            if (this.endDate && currentDate.getTime() === this.endDate.getTime()) {
                classes += ' selected-end';
            }
            // Check if this day is in range
            if (this.startDate && this.endDate && currentDate > this.startDate && currentDate < this.endDate) {
                classes += ' in-range';
            }
            
            html += '<div class="' + classes + '" data-date="' + currentDate.toISOString() + '">' + day + '</div>';
        }
        
        html += '</div></div>';
        
        document.getElementById(containerId).innerHTML = html;
    }
    
    renderCalendars() {
        const firstMonth = new Date(this.today.getFullYear(), this.today.getMonth() + this.currentMonthOffset, 1);
        const secondMonth = new Date(this.today.getFullYear(), this.today.getMonth() + this.currentMonthOffset + 1, 1);
        
        this.generateCalendar(this.calendar1Id, firstMonth);
        this.generateCalendar(this.calendar2Id, secondMonth);
        
        // Disable prev button if we're at current month
        document.getElementById(this.prevMonthButtonId).disabled = this.currentMonthOffset <= 0;
    }
    
    updateDisplay() {
        const displayEl = document.getElementById(this.displayElementId);
        const startInput = document.getElementById(this.startDateInputId);
        const endInput = document.getElementById(this.endDateInputId);
        
        if (this.startDate && this.endDate) {
            const options = { month: 'short', day: 'numeric' };
            const startStr = this.startDate.toLocaleDateString('en-US', options);
            const endStr = this.endDate.toLocaleDateString('en-US', options);
            displayEl.textContent = startStr + ' - ' + endStr;
            
            startInput.value = this.formatDate(this.startDate);
            endInput.value = this.formatDate(this.endDate);
        } else if (this.startDate) {
            const options = { month: 'short', day: 'numeric' };
            displayEl.textContent = this.startDate.toLocaleDateString('en-US', options) + ' - ...';
        } else {
            displayEl.textContent = this.defaultDisplayText;
            startInput.value = '';
            endInput.value = '';
        }
    }
    
    formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }
    
    // Public methods
    getStartDate() {
        return this.startDate;
    }
    
    getEndDate() {
        return this.endDate;
    }
    
    setDateRange(startDate, endDate) {
        this.startDate = startDate ? new Date(startDate) : null;
        this.endDate = endDate ? new Date(endDate) : null;
        if (this.startDate) this.startDate.setHours(0, 0, 0, 0);
        if (this.endDate) this.endDate.setHours(0, 0, 0, 0);
        this.updateDisplay();
    }
    
    clearDateRange() {
        this.startDate = null;
        this.endDate = null;
        this.updateDisplay();
    }
    
    confirmSelection() {
        if (this.startDate && this.endDate) {
            document.getElementById(this.pickerContainerId).style.display = 'none';
            
            // Call callbacks if provided
            if (this.onDateRangeChange) {
                this.onDateRangeChange(this.startDate, this.endDate);
            }
            if (this.onConfirm) {
                this.onConfirm(this.startDate, this.endDate);
            }
        }
    }
    
    updateConfirmButton() {
        if (this.confirmButtonId) {
            const button = document.getElementById(this.confirmButtonId);
            if (button) {
                button.disabled = !(this.startDate && this.endDate);
            }
        }
    }
    
    setDisabledDates(disabledDates) {
        this.disabledDates = disabledDates || [];
        this.renderCalendars();
    }
    
    hasDisabledDatesInRange(startDate, endDate) {
        let currentDate = new Date(startDate);
        while (currentDate <= endDate) {
            const dateStr = this.formatDate(currentDate);
            if (this.disabledDates.includes(dateStr)) {
                return true;
            }
            currentDate.setDate(currentDate.getDate() + 1);
        }
        return false;
    }
}
